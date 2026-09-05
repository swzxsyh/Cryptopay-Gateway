package watcher

import (
	"context"
	"fmt"
	"math/big"
	"strings"
	"sync"
	"time"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/model"
	"github.com/ethereum/go-ethereum"
	"github.com/ethereum/go-ethereum/common"
	"github.com/ethereum/go-ethereum/core/types"
	"github.com/ethereum/go-ethereum/crypto"
	"github.com/ethereum/go-ethereum/ethclient"
)

var subscriptionEventTopics = map[common.Hash]string{
	crypto.Keccak256Hash([]byte("SubscriptionActivated(bytes32,address,address)")):           "ACTIVATED",
	crypto.Keccak256Hash([]byte("SubscriptionBillingPaid(bytes32,uint256,address,uint256)")): "BILLING_PAID",
	crypto.Keccak256Hash([]byte("SubscriptionPaused(bytes32)")):                              "PAUSED",
	crypto.Keccak256Hash([]byte("SubscriptionResumed(bytes32)")):                             "RESUMED",
	crypto.Keccak256Hash([]byte("SubscriptionCancelled(bytes32)")):                           "CANCELLED",
}

// SubscriptionWatcher scans EVM subscription contracts and emits normalized lifecycle events.
type SubscriptionWatcher struct {
	mu      sync.Mutex
	started map[string]struct{}
}

func (w *SubscriptionWatcher) Name() string { return "subscription" }

func (w *SubscriptionWatcher) Supports(profile config.ChainProfile) bool {
	return strings.EqualFold(profile.Family, "EVM")
}

func (w *SubscriptionWatcher) Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	if !deps.Config.Subscription.Enabled || len(deps.Config.Subscription.Contracts) == 0 {
		return nil
	}
	w.mu.Lock()
	if w.started == nil {
		w.started = make(map[string]struct{})
	}
	key := strings.ToUpper(profile.Chain)
	if _, ok := w.started[key]; ok {
		w.mu.Unlock()
		return nil
	}
	w.started[key] = struct{}{}
	w.mu.Unlock()

	go w.run(ctx, deps, profile)
	return nil
}

func (w *SubscriptionWatcher) Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	if !deps.Config.Subscription.Enabled || len(deps.Config.Subscription.Contracts) == 0 {
		return nil
	}
	endpoint := firstText(profile.RpcURL, profile.WsURL)
	if endpoint == "" {
		return nil
	}
	client, err := ethclient.DialContext(ctx, endpoint)
	if err != nil {
		return err
	}
	defer client.Close()
	return w.scanOnce(ctx, deps, profile, client)
}

func (w *SubscriptionWatcher) run(ctx context.Context, deps Dependencies, profile config.ChainProfile) {
	logger := deps.LoggerOrDefault()
	endpoint := firstText(profile.RpcURL, profile.WsURL)
	if endpoint == "" {
		logger.Warn("subscription watcher has no rpc/ws endpoint", "chain", profile.Chain)
		return
	}
	client, err := ethclient.DialContext(ctx, endpoint)
	if err != nil {
		logger.Warn("subscription watcher dial failed", "chain", profile.Chain, "error", err)
		return
	}
	defer client.Close()

	interval := chainScanInterval(deps, profile)
	if interval <= 0 {
		interval = 15 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	logger.Info("subscription watcher started", "chain", profile.Chain, "contractCount", len(deps.Config.Subscription.Contracts))
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if err := w.scanWithLock(ctx, deps, profile, client); err != nil {
				logger.Warn("subscription scan failed", "chain", profile.Chain, "error", err)
			}
		}
	}
}

func (w *SubscriptionWatcher) scanWithLock(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *ethclient.Client) error {
	lease := time.Duration(maxInt(deps.Config.Lock.LeaseSeconds, 10)) * time.Second
	lockKey := deps.Config.Redis.Prefix + ":scanner:subscription:" + strings.ToUpper(strings.TrimSpace(profile.Chain))
	if deps.Locker == nil {
		return w.scanOnce(ctx, deps, profile, client)
	}
	return deps.Locker.WithLock(ctx, lockKey, lease, func() error {
		return w.scanOnce(ctx, deps, profile, client)
	})
}

func (w *SubscriptionWatcher) scanOnce(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *ethclient.Client) error {
	latest, err := client.BlockNumber(ctx)
	if err != nil {
		return err
	}
	depth := profile.ConfirmationDepth
	if depth <= 0 {
		depth = 12
	}
	confirmedHeight := int64(latest) - depth
	if confirmedHeight < 1 {
		return nil
	}
	checkpointChain := profile.Chain + ":SUBSCRIPTION"
	var lastConfirmed int64
	if deps.Checkpoint != nil {
		lastConfirmed, _ = deps.Checkpoint.ConfirmedHeight(ctx, checkpointChain)
	}
	if lastConfirmed <= 0 {
		_ = deps.Checkpoint.UpdateConfirmed(ctx, checkpointChain, maxInt64(1, confirmedHeight-maxInt64(chainBackfillBlocks(deps, profile), 12)))
		deps.LoggerOrDefault().Info("subscription checkpoint initialized", "chain", profile.Chain, "confirmedHeight", confirmedHeight)
		return nil
	}
	startBlock := maxInt64(lastConfirmed+1-maxInt64(chainBackfillBlocks(deps, profile), 12), 1)
	if startBlock > confirmedHeight {
		return nil
	}
	batchSize := maxInt64(chainLogBatchBlocks(deps, profile), 50)
	contracts := subscriptionContracts(deps.Config.Subscription.Contracts)
	for batchStart := startBlock; batchStart <= confirmedHeight; batchStart += batchSize {
		batchEnd := minInt64(confirmedHeight, batchStart+batchSize-1)
		if err := w.scanBatch(ctx, deps, profile, client, contracts, batchStart, batchEnd); err != nil {
			return err
		}
		if deps.Checkpoint != nil {
			_ = deps.Checkpoint.UpdateConfirmed(ctx, checkpointChain, batchEnd)
		}
	}
	return nil
}

func (w *SubscriptionWatcher) scanBatch(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *ethclient.Client, contracts []common.Address, fromBlock, toBlock int64) error {
	if len(contracts) == 0 || deps.SubPublisher == nil {
		return nil
	}
	query := ethereum.FilterQuery{
		FromBlock: big.NewInt(fromBlock),
		ToBlock:   big.NewInt(toBlock),
		Addresses: contracts,
		Topics:    [][]common.Hash{subscriptionTopicList()},
	}
	logs, err := client.FilterLogs(ctx, query)
	if err != nil {
		return err
	}
	headers := make(map[uint64]*types.Header)
	for _, lg := range logs {
		eventType, ok := subscriptionEventTopics[lg.Topics[0]]
		if !ok || len(lg.Topics) < 2 {
			continue
		}
		header := headers[lg.BlockNumber]
		if header == nil {
			header, err = client.HeaderByNumber(ctx, big.NewInt(int64(lg.BlockNumber)))
			if err != nil {
				return err
			}
			headers[lg.BlockNumber] = header
		}
		event := parseSubscriptionLog(profile, lg, eventType, time.Unix(int64(header.Time), 0).UTC())
		deps.LoggerOrDefault().Info("subscription event published", "chain", event.Chain, "eventType", event.EventType, "subscriptionId", event.SubscriptionID, "txHash", event.TxHash, "blockNumber", event.BlockNumber)
		if err := deps.SubPublisher.PublishSubscription(ctx, event); err != nil {
			return err
		}
	}
	return nil
}

func parseSubscriptionLog(profile config.ChainProfile, lg types.Log, eventType string, blockTime time.Time) model.SubscriptionEvent {
	subscriptionID := lg.Topics[1].Hex()
	sequence, amount := "", ""
	if eventType == "BILLING_PAID" && len(lg.Data) >= 96 {
		sequence = new(big.Int).SetBytes(lg.Data[0:32]).String()
		amount = new(big.Int).SetBytes(lg.Data[64:96]).String()
	}
	return model.SubscriptionEvent{
		EventID:        fmt.Sprintf("subscription:%s:%s:%d", strings.ToLower(profile.Chain), strings.ToLower(lg.TxHash.Hex()), lg.Index),
		Source:         "EVM_SUBSCRIPTION_GET_LOGS",
		Chain:          profile.Chain,
		Contract:       lg.Address.Hex(),
		EventType:      eventType,
		SubscriptionID: subscriptionID,
		TxHash:         lg.TxHash.Hex(),
		LogIndex:       int64(lg.Index),
		BlockNumber:    int64(lg.BlockNumber),
		BlockTimestamp: blockTime,
		Sequence:       sequence,
		AmountRaw:      amount,
		ObservedAt:     time.Now().UTC(),
	}
}

func subscriptionContracts(raw []string) []common.Address {
	out := make([]common.Address, 0, len(raw))
	seen := map[string]struct{}{}
	for _, value := range raw {
		value = strings.TrimSpace(value)
		if !common.IsHexAddress(value) {
			continue
		}
		key := strings.ToLower(value)
		if _, ok := seen[key]; ok {
			continue
		}
		seen[key] = struct{}{}
		out = append(out, common.HexToAddress(value))
	}
	return out
}

func subscriptionTopicList() []common.Hash {
	topics := make([]common.Hash, 0, len(subscriptionEventTopics))
	for topic := range subscriptionEventTopics {
		topics = append(topics, topic)
	}
	return topics
}
