package watcher

import (
	"context"
	"math/big"
	"strings"
	"sync"
	"time"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/model"
	solana "github.com/gagliardetto/solana-go"
	"github.com/gagliardetto/solana-go/rpc"
)

// SolanaWatcher scans finalized SPL token transfers by destination address.
// It is isolated from the EVM watcher so Solana protocol changes do not affect EVM receipt matching.
type SolanaWatcher struct {
	mu      sync.Mutex
	started map[string]struct{}
}

func (w *SolanaWatcher) Name() string { return "solana" }

func (w *SolanaWatcher) Supports(profile config.ChainProfile) bool {
	return strings.EqualFold(profile.Family, "SOLANA")
}

func (w *SolanaWatcher) Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	w.mu.Lock()
	if w.started == nil {
		w.started = make(map[string]struct{})
	}
	if _, ok := w.started[profile.Chain]; ok {
		w.mu.Unlock()
		return nil
	}
	w.started[profile.Chain] = struct{}{}
	w.mu.Unlock()

	go w.run(ctx, deps, profile)
	return nil
}

func (w *SolanaWatcher) Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	return w.scanOnce(ctx, deps, profile, rpc.New(profile.RpcURL))
}

func (w *SolanaWatcher) run(ctx context.Context, deps Dependencies, profile config.ChainProfile) {
	logger := deps.LoggerOrDefault()
	if strings.TrimSpace(profile.RpcURL) == "" {
		logger.Warn("solana watcher has no rpc endpoint", "chain", profile.Chain)
		return
	}
	client := rpc.New(profile.RpcURL)
	interval := chainScanInterval(deps, profile)
	if interval <= 0 {
		interval = 15 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	logger.Info("solana watcher started", "chain", profile.Chain, "rpcUrl", profile.RpcURL)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if err := w.scanWithLock(ctx, deps, profile, client); err != nil {
				logger.Warn("solana scan failed", "chain", profile.Chain, "error", err)
			}
		}
	}
}

func (w *SolanaWatcher) scanWithLock(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *rpc.Client) error {
	lease := time.Duration(maxInt(deps.Config.Lock.LeaseSeconds, 10)) * time.Second
	lockKey := deps.Config.Redis.Prefix + ":scanner:solana:" + strings.ToUpper(strings.TrimSpace(profile.Chain))
	if deps.Locker == nil {
		return w.scanOnce(ctx, deps, profile, client)
	}
	return deps.Locker.WithLock(ctx, lockKey, lease, func() error {
		return w.scanOnce(ctx, deps, profile, client)
	})
}

func (w *SolanaWatcher) scanOnce(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *rpc.Client) error {
	logger := deps.LoggerOrDefault()
	rules := matchingRules(profile, deps.Config.Rules)
	if len(rules) == 0 {
		logger.Debug("solana watcher has no active rules", "chain", profile.Chain)
		return nil
	}
	latestSlot, err := client.GetSlot(ctx, rpc.CommitmentFinalized)
	if err != nil {
		return err
	}
	if deps.Checkpoint != nil {
		_ = deps.Checkpoint.UpdateObserved(ctx, profile.Chain, int64(latestSlot))
	}

	lastConfirmed := int64(0)
	if deps.Checkpoint != nil {
		lastConfirmed, _ = deps.Checkpoint.ConfirmedHeight(ctx, profile.Chain)
	}
	if lastConfirmed <= 0 {
		startAt := int64(latestSlot) - maxInt64(chainBackfillBlocks(deps, profile), 12)
		if startAt < 1 {
			startAt = int64(latestSlot)
		}
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, startAt)
		logger.Info("solana checkpoint initialized", "chain", profile.Chain, "confirmedSlot", startAt)
		return nil
	}

	maxSeen := lastConfirmed
	for _, rule := range rules {
		if !strings.EqualFold(rule.Family, "") && !strings.EqualFold(rule.Family, "SOLANA") {
			continue
		}
		mint, err := solana.PublicKeyFromBase58(strings.TrimSpace(rule.TokenAddress))
		if err != nil {
			logger.Warn("invalid solana token mint", "chain", profile.Chain, "tokenAddress", rule.TokenAddress, "error", err)
			continue
		}
		destinations := destinationAddresses(ctx, deps, profile.Chain, rule)
		if len(destinations) == 0 {
			logger.Debug("solana scan skipped because no active destination exists", "chain", profile.Chain, "tokenAddress", rule.TokenAddress)
			continue
		}
		for _, destination := range destinations {
			pubkey, err := solana.PublicKeyFromBase58(strings.TrimSpace(destination))
			if err != nil {
				logger.Warn("invalid solana destination address", "chain", profile.Chain, "destination", destination, "error", err)
				continue
			}
			seen, err := w.scanDestination(ctx, deps, profile, client, rule, mint, pubkey, lastConfirmed)
			if err != nil {
				return err
			}
			if seen > maxSeen {
				maxSeen = seen
			}
		}
	}
	if deps.Checkpoint != nil && maxSeen > lastConfirmed {
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, maxSeen)
	}
	return nil
}

func (w *SolanaWatcher) scanDestination(
	ctx context.Context,
	deps Dependencies,
	profile config.ChainProfile,
	client *rpc.Client,
	rule config.WatchRule,
	mint solana.PublicKey,
	destination solana.PublicKey,
	lastConfirmed int64,
) (int64, error) {
	limit := int(maxInt64(chainLogBatchBlocks(deps, profile), 100))
	if limit > 1000 {
		limit = 1000
	}
	signatures, err := client.GetSignaturesForAddressWithOpts(ctx, destination, &rpc.GetSignaturesForAddressOpts{
		Limit:      &limit,
		Commitment: rpc.CommitmentFinalized,
	})
	if err != nil {
		return lastConfirmed, err
	}
	maxSeen := lastConfirmed
	maxTxVersion := uint64(0)
	for i := len(signatures) - 1; i >= 0; i-- {
		sig := signatures[i]
		if sig == nil || sig.Err != nil || int64(sig.Slot) <= lastConfirmed {
			continue
		}
		if int64(sig.Slot) > maxSeen {
			maxSeen = int64(sig.Slot)
		}
		tx, err := client.GetParsedTransaction(ctx, sig.Signature, &rpc.GetParsedTransactionOpts{
			Commitment:                     rpc.CommitmentFinalized,
			MaxSupportedTransactionVersion: &maxTxVersion,
		})
		if err != nil {
			return maxSeen, err
		}
		if tx == nil || tx.Meta == nil || tx.Meta.Err != nil {
			continue
		}
		amount, sourceAddress := solanaTokenTransferDiff(tx.Meta.PreTokenBalances, tx.Meta.PostTokenBalances, mint, destination)
		if amount.Sign() <= 0 {
			continue
		}
		blockTime := time.Now().UTC()
		if tx.BlockTime != nil {
			blockTime = time.Unix(int64(*tx.BlockTime), 0).UTC()
		}
		event := model.ChainPaymentEvent{
			EventID:            chainEventID("SOLANA_SPL_SCAN", profile.Chain, sig.Signature.String(), destination.String(), rule.TokenAddress),
			Source:             "SOLANA_SPL_SCAN",
			Chain:              profile.Chain,
			Token:              defaultToken(rule.Token),
			TokenAddress:       rule.TokenAddress,
			TxHash:             sig.Signature.String(),
			SourceAddress:      sourceAddress,
			DestinationAddress: destination.String(),
			Amount:             formatRatAmount(amount, rule.Decimals),
			BlockNumber:        int64(sig.Slot),
			LogIndex:           0,
			BlockTimestamp:     blockTime,
			ObservedAt:         time.Now().UTC(),
		}
		deps.LoggerOrDefault().Info(
			"solana spl transfer published",
			"chain", event.Chain,
			"token", event.Token,
			"mint", event.TokenAddress,
			"txHash", event.TxHash,
			"destinationAddress", event.DestinationAddress,
			"amount", event.Amount,
			"slot", event.BlockNumber,
		)
		if deps.Publisher != nil {
			if err := deps.Publisher.Publish(ctx, event); err != nil {
				return maxSeen, err
			}
		}
	}
	return maxSeen, nil
}

func solanaTokenTransferDiff(pre []rpc.TokenBalance, post []rpc.TokenBalance, mint solana.PublicKey, owner solana.PublicKey) (*big.Rat, string) {
	before := tokenBalanceSum(pre, mint, owner)
	after := tokenBalanceSum(post, mint, owner)
	increase := after.Sub(after, before)
	return increase, solanaTokenDecreaseOwner(pre, post, mint)
}

func solanaTokenDecreaseOwner(pre []rpc.TokenBalance, post []rpc.TokenBalance, mint solana.PublicKey) string {
	beforeByOwner := tokenBalancesByOwner(pre, mint)
	afterByOwner := tokenBalancesByOwner(post, mint)
	for owner, before := range beforeByOwner {
		after := afterByOwner[owner]
		if after == nil {
			after = new(big.Rat)
		}
		delta := new(big.Rat).Sub(after, before)
		if delta.Sign() < 0 {
			return owner
		}
	}
	return ""
}

func tokenBalanceSum(balances []rpc.TokenBalance, mint solana.PublicKey, owner solana.PublicKey) *big.Rat {
	total := new(big.Rat)
	for _, balance := range balances {
		if balance.Owner == nil || balance.UiTokenAmount == nil {
			continue
		}
		if balance.Mint != mint || *balance.Owner != owner {
			continue
		}
		amount := new(big.Int)
		if _, ok := amount.SetString(balance.UiTokenAmount.Amount, 10); !ok {
			continue
		}
		decimals := int64(balance.UiTokenAmount.Decimals)
		denom := new(big.Int).Exp(big.NewInt(10), big.NewInt(decimals), nil)
		total.Add(total, new(big.Rat).SetFrac(amount, denom))
	}
	return total
}

func tokenBalancesByOwner(balances []rpc.TokenBalance, mint solana.PublicKey) map[string]*big.Rat {
	out := make(map[string]*big.Rat)
	for _, balance := range balances {
		if balance.Owner == nil || balance.UiTokenAmount == nil || balance.Mint != mint {
			continue
		}
		owner := balance.Owner.String()
		amount := new(big.Int)
		if _, ok := amount.SetString(balance.UiTokenAmount.Amount, 10); !ok {
			continue
		}
		denom := new(big.Int).Exp(big.NewInt(10), big.NewInt(int64(balance.UiTokenAmount.Decimals)), nil)
		value := new(big.Rat).SetFrac(amount, denom)
		if out[owner] == nil {
			out[owner] = new(big.Rat)
		}
		out[owner].Add(out[owner], value)
	}
	return out
}

func formatRatAmount(amount *big.Rat, decimals int) string {
	if amount == nil {
		return "0"
	}
	if decimals < 0 {
		decimals = 0
	}
	if decimals == 0 {
		return amount.FloatString(0)
	}
	return amount.FloatString(decimals)
}
