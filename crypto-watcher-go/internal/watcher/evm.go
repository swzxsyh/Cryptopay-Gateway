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

var evmTransferTopic = crypto.Keccak256Hash([]byte("Transfer(address,address,uint256)"))

// EVMWatcher scans ERC20 transfer logs and keeps checkpoint / leader semantics.
type EVMWatcher struct {
	mu      sync.Mutex
	started map[string]struct{}
}

func (w *EVMWatcher) Name() string { return "evm" }

func (w *EVMWatcher) Supports(profile config.ChainProfile) bool {
	return strings.EqualFold(profile.Family, "EVM")
}

func (w *EVMWatcher) Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	if len(matchingRules(profile, deps.Config.Rules)) == 0 {
		(deps.LoggerOrDefault()).Warn("no EVM watch rules configured", "chain", profile.Chain)
	}
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

func (w *EVMWatcher) Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
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

func (w *EVMWatcher) run(ctx context.Context, deps Dependencies, profile config.ChainProfile) {
	logger := deps.LoggerOrDefault()
	cooldown := 5 * time.Second
	for ctx.Err() == nil {
		if err := w.runSession(ctx, deps, profile); err != nil && ctx.Err() == nil {
			logger.Warn("evm watcher session stopped, retrying after cooldown", "chain", profile.Chain, "cooldown", cooldown.String(), "error", err)
		}
		select {
		case <-ctx.Done():
			return
		case <-time.After(cooldown):
		}
	}
}

func (w *EVMWatcher) runSession(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	logger := deps.LoggerOrDefault()
	endpoint := firstText(profile.WsURL, profile.RpcURL)
	if endpoint == "" {
		logger.Warn("evm watcher has no rpc/ws endpoint", "chain", profile.Chain)
		return nil
	}

	client, err := ethclient.DialContext(ctx, endpoint)
	if err != nil {
		return err
	}
	defer client.Close()

	if strings.TrimSpace(profile.WsURL) != "" && chainWebsocketEnabled(deps, profile) {
		go w.watchHeads(ctx, deps, profile, client)
	}

	interval := chainScanInterval(deps, profile)
	if interval <= 0 {
		interval = 15 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	logger.Info("evm watcher started", "chain", profile.Chain, "endpoint", endpoint)
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case <-ticker.C:
			if !w.ensureLeader(ctx, deps, profile) {
				continue
			}
			if err := w.scanWithLock(ctx, deps, profile, client); err != nil {
				logger.Warn("evm scan failed", "chain", profile.Chain, "error", err)
			}
		}
	}
}

func (w *EVMWatcher) watchHeads(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *ethclient.Client) {
	logger := deps.LoggerOrDefault()
	for ctx.Err() == nil {
		headers := make(chan *types.Header, 128)
		sub, err := client.SubscribeNewHead(ctx, headers)
		if err != nil {
			logger.Warn("evm websocket head subscription failed", "chain", profile.Chain, "error", err)
			sleepOrDone(ctx, 3*time.Second)
			continue
		}
		logger.Info("evm websocket head subscription started", "chain", profile.Chain)
		closed := false
		for !closed {
			select {
			case <-ctx.Done():
				sub.Unsubscribe()
				return
			case err := <-sub.Err():
				sub.Unsubscribe()
				closed = true
				if err != nil {
					logger.Warn("evm websocket head subscription closed", "chain", profile.Chain, "error", err)
				}
			case header := <-headers:
				if header == nil || header.Number == nil {
					continue
				}
				if !w.isCurrentLeader(ctx, deps, profile) {
					continue
				}
				if deps.Checkpoint != nil {
					_ = deps.Checkpoint.UpdateObserved(ctx, profile.Chain, header.Number.Int64())
				}
				logger.Debug("evm websocket head observed", "chain", profile.Chain, "blockNumber", header.Number.Int64())
			}
		}
		sleepOrDone(ctx, 3*time.Second)
	}
}

func sleepOrDone(ctx context.Context, duration time.Duration) {
	select {
	case <-ctx.Done():
	case <-time.After(duration):
	}
}

func (w *EVMWatcher) scanWithLock(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *ethclient.Client) error {
	lease := time.Duration(maxInt(deps.Config.Lock.LeaseSeconds, 10)) * time.Second
	lockKey := deps.Config.Redis.Prefix + ":scanner:lock:" + strings.ToUpper(strings.TrimSpace(profile.Chain))
	if deps.Locker == nil {
		return w.scanOnce(ctx, deps, profile, client)
	}
	return deps.Locker.WithLock(ctx, lockKey, lease, func() error {
		return w.scanOnce(ctx, deps, profile, client)
	})
}

func (w *EVMWatcher) scanOnce(ctx context.Context, deps Dependencies, profile config.ChainProfile, client *ethclient.Client) error {
	logger := deps.LoggerOrDefault()
	rules := matchingRules(profile, deps.Config.Rules)
	if len(rules) == 0 {
		logger.Debug("evm watcher has no active rules", "chain", profile.Chain)
		return nil
	}

	latest, err := client.BlockNumber(ctx)
	if err != nil {
		return err
	}
	if deps.Checkpoint != nil {
		_ = deps.Checkpoint.UpdateObserved(ctx, profile.Chain, int64(latest))
	}

	confirmationDepth := profile.ConfirmationDepth
	if confirmationDepth <= 0 {
		confirmationDepth = 12
	}
	confirmedHeight := int64(latest) - confirmationDepth
	if confirmedHeight < 1 {
		return nil
	}

	lastConfirmed := int64(0)
	if deps.Checkpoint != nil {
		lastConfirmed, _ = deps.Checkpoint.ConfirmedHeight(ctx, profile.Chain)
	}
	if lastConfirmed <= 0 {
		if deps.Checkpoint != nil {
			_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, confirmedHeight)
		}
		logger.Info("evm checkpoint initialized", "chain", profile.Chain, "confirmedHeight", confirmedHeight)
		return nil
	}

	backfillBlocks := maxInt64(chainBackfillBlocks(deps, profile), 12)
	startBlock := maxInt64(lastConfirmed+1-backfillBlocks, 1)
	if startBlock > confirmedHeight {
		startBlock = maxInt64(confirmedHeight-backfillBlocks+1, 1)
	}
	if startBlock > confirmedHeight {
		return nil
	}

	batchSize := maxInt64(chainLogBatchBlocks(deps, profile), 50)
	for batchStart := startBlock; batchStart <= confirmedHeight; batchStart += batchSize {
		batchEnd := minInt64(confirmedHeight, batchStart+batchSize-1)
		if err := w.scanBatch(ctx, deps, profile, client, rules, batchStart, batchEnd); err != nil {
			return err
		}
		if deps.Checkpoint != nil {
			_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, batchEnd)
		}
	}
	return nil
}

func (w *EVMWatcher) scanBatch(
	ctx context.Context,
	deps Dependencies,
	profile config.ChainProfile,
	client *ethclient.Client,
	rules []config.WatchRule,
	fromBlock int64,
	toBlock int64,
) error {
	logger := deps.LoggerOrDefault()
	for _, rule := range rules {
		if !strings.EqualFold(rule.Family, "") && !strings.EqualFold(rule.Family, "EVM") {
			continue
		}
		if strings.TrimSpace(rule.TokenAddress) == "" {
			continue
		}
		destinations := destinationAddresses(ctx, deps, profile.Chain, rule)
		if len(destinations) == 0 {
			logger.Debug("evm scan skipped because no active destination exists", "chain", profile.Chain, "tokenAddress", rule.TokenAddress)
			continue
		}
		query := ethereum.FilterQuery{
			FromBlock: big.NewInt(fromBlock),
			ToBlock:   big.NewInt(toBlock),
			Addresses: []common.Address{common.HexToAddress(rule.TokenAddress)},
			Topics:    buildTopics(destinations),
		}

		logs, err := w.queryLogsWithRetry(ctx, deps, profile, client, rule, query, fromBlock, toBlock)
		if err != nil {
			return err
		}
		if len(logs) == 0 {
			continue
		}

		headers := make(map[uint64]*types.Header)
		for _, lg := range logs {
			if len(lg.Topics) < 3 || lg.TxHash == (common.Hash{}) {
				continue
			}
			blockHeader, ok := headers[lg.BlockNumber]
			if !ok {
				blockHeader, err = client.HeaderByNumber(ctx, big.NewInt(int64(lg.BlockNumber)))
				if err != nil {
					return err
				}
				headers[lg.BlockNumber] = blockHeader
			}
			event := model.ChainPaymentEvent{
				EventID:            chainEventID("EVM_ERC20_GET_LOGS", profile.Chain, lg.TxHash.Hex(), topicToAddress(lg.Topics[2]), rule.TokenAddress),
				Source:             "EVM_ERC20_GET_LOGS",
				Chain:              profile.Chain,
				Token:              defaultToken(rule.Token),
				TokenAddress:       rule.TokenAddress,
				TxHash:             lg.TxHash.Hex(),
				SourceAddress:      topicToAddress(lg.Topics[1]),
				DestinationAddress: topicToAddress(lg.Topics[2]),
				Amount:             formatEvmAmount(lg.Data, rule.Decimals),
				BlockNumber:        int64(lg.BlockNumber),
				LogIndex:           int64(lg.Index),
				BlockTimestamp:     time.Unix(int64(blockHeader.Time), 0).UTC(),
				ObservedAt:         time.Now().UTC(),
			}
			logger.Info(
				"evm transfer published",
				"chain", event.Chain,
				"token", event.Token,
				"tokenAddress", event.TokenAddress,
				"txHash", event.TxHash,
				"sourceAddress", event.SourceAddress,
				"destinationAddress", event.DestinationAddress,
				"amount", event.Amount,
				"blockNumber", event.BlockNumber,
			)
			if deps.Publisher != nil {
				if err := deps.Publisher.Publish(ctx, event); err != nil {
					return err
				}
			}
		}
	}
	return nil
}

func (w *EVMWatcher) queryLogsWithRetry(
	ctx context.Context,
	deps Dependencies,
	profile config.ChainProfile,
	client *ethclient.Client,
	rule config.WatchRule,
	query ethereum.FilterQuery,
	fromBlock int64,
	toBlock int64,
) ([]types.Log, error) {
	attempts := maxInt(chainRetryAttempts(deps, profile), 3)
	var lastErr error
	for attempt := 1; attempt <= attempts; attempt++ {
		logs, err := client.FilterLogs(ctx, query)
		if err == nil {
			return logs, nil
		}
		lastErr = err
		deps.LoggerOrDefault().Warn(
			"evm filter logs failed",
			"chain", profile.Chain,
			"tokenAddress", rule.TokenAddress,
			"fromBlock", fromBlock,
			"toBlock", toBlock,
			"attempt", attempt,
			"maxAttempts", attempts,
			"error", err,
		)
		time.Sleep(time.Duration(attempt) * 500 * time.Millisecond)
	}
	return nil, lastErr
}

func (w *EVMWatcher) ensureLeader(ctx context.Context, deps Dependencies, profile config.ChainProfile) bool {
	if deps.Leader == nil {
		return true
	}
	lease := chainLeaderLease(deps, profile)
	current, err := deps.Leader.Current(ctx, profile.Chain)
	if err == nil && strings.TrimSpace(current) == deps.NodeID && deps.NodeID != "" {
		ok, renewErr := deps.Leader.Renew(ctx, profile.Chain, lease)
		if renewErr == nil && ok {
			return true
		}
	}
	ok, err := deps.Leader.TryAcquire(ctx, profile.Chain, lease)
	if err != nil {
		deps.LoggerOrDefault().Warn("evm leader acquire failed", "chain", profile.Chain, "error", err)
		return false
	}
	return ok
}

func (w *EVMWatcher) isCurrentLeader(ctx context.Context, deps Dependencies, profile config.ChainProfile) bool {
	if deps.Leader == nil {
		return true
	}
	current, err := deps.Leader.Current(ctx, profile.Chain)
	return err == nil && strings.TrimSpace(current) == deps.NodeID && deps.NodeID != ""
}

func buildTopics(destinations []string) [][]common.Hash {
	topics := [][]common.Hash{{evmTransferTopic}}
	if len(destinations) == 0 {
		return topics
	}
	dstTopics := make([]common.Hash, 0, len(destinations))
	for _, dst := range destinations {
		dst = cleanAddress(dst)
		if dst == "" {
			continue
		}
		dstTopics = append(dstTopics, addressToTopic(dst))
	}
	if len(dstTopics) > 0 {
		topics = append(topics, nil, dstTopics)
	}
	return topics
}

func addressToTopic(address string) common.Hash {
	address = cleanAddress(address)
	if !strings.HasPrefix(address, "0x") {
		address = "0x" + address
	}
	return common.HexToHash("0x000000000000000000000000" + strings.TrimPrefix(address, "0x"))
}

func topicToAddress(topic common.Hash) string {
	bytes := topic.Bytes()
	if len(bytes) < 20 {
		return ""
	}
	return common.BytesToAddress(bytes[len(bytes)-20:]).Hex()
}

func formatEvmAmount(data []byte, decimals int) string {
	if decimals < 0 {
		decimals = 0
	}
	raw := new(big.Int).SetBytes(data)
	if raw.Sign() == 0 {
		return "0"
	}
	if decimals == 0 {
		return raw.String()
	}
	denom := new(big.Int).Exp(big.NewInt(10), big.NewInt(int64(decimals)), nil)
	return new(big.Rat).SetFrac(raw, denom).FloatString(decimals)
}

func defaultToken(token string) string {
	token = strings.TrimSpace(token)
	if token == "" {
		return "UNKNOWN"
	}
	return token
}

func firstText(values ...string) string {
	for _, value := range values {
		if strings.TrimSpace(value) != "" {
			return strings.TrimSpace(value)
		}
	}
	return ""
}

func chainEventID(source, chain, txHash, destinationAddress, tokenAddress string) string {
	parts := []string{source, chain, txHash, destinationAddress, tokenAddress}
	for i, part := range parts {
		parts[i] = strings.ToLower(strings.TrimSpace(part))
	}
	seed := strings.Join(parts, ":")
	if strings.ReplaceAll(seed, ":", "") == "" {
		return fmt.Sprintf("go-watcher-%d", time.Now().UnixNano())
	}
	return seed
}

func maxInt(values ...int) int {
	if len(values) == 0 {
		return 0
	}
	max := values[0]
	for _, value := range values[1:] {
		if value > max {
			max = value
		}
	}
	return max
}

func maxInt64(values ...int64) int64 {
	if len(values) == 0 {
		return 0
	}
	max := values[0]
	for _, value := range values[1:] {
		if value > max {
			max = value
		}
	}
	return max
}

func minInt64(a, b int64) int64 {
	if a < b {
		return a
	}
	return b
}
