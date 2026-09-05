package watcher

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"sync"
	"time"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/model"
)

// SuiWatcher scans SUI Move events by configured CoinType / MoveEventType.
type SuiWatcher struct {
	mu      sync.Mutex
	started map[string]struct{}
	client  *http.Client
}

func (w *SuiWatcher) Name() string { return "sui" }

func (w *SuiWatcher) Supports(profile config.ChainProfile) bool {
	return strings.EqualFold(profile.Family, "SUI")
}

func (w *SuiWatcher) Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	w.mu.Lock()
	if w.started == nil {
		w.started = make(map[string]struct{})
	}
	if w.client == nil {
		w.client = defaultHTTPClient()
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

func (w *SuiWatcher) Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	return w.scanWithLock(ctx, deps, profile)
}

func (w *SuiWatcher) run(ctx context.Context, deps Dependencies, profile config.ChainProfile) {
	logger := deps.LoggerOrDefault()
	if strings.TrimSpace(profile.RpcURL) == "" {
		logger.Warn("sui watcher has no rpc endpoint", "chain", profile.Chain)
		return
	}
	interval := chainScanInterval(deps, profile)
	if interval <= 0 {
		interval = 15 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	logger.Info("sui watcher started", "chain", profile.Chain, "rpcUrl", profile.RpcURL)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if err := w.scanWithLock(ctx, deps, profile); err != nil {
				logger.Warn("sui scan failed", "chain", profile.Chain, "error", err)
			}
		}
	}
}

func (w *SuiWatcher) scanWithLock(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	lease := time.Duration(maxInt(deps.Config.Lock.LeaseSeconds, 10)) * time.Second
	lockKey := deps.Config.Redis.Prefix + ":scanner:sui:" + strings.ToUpper(strings.TrimSpace(profile.Chain))
	if deps.Locker == nil {
		return w.scanOnce(ctx, deps, profile)
	}
	return deps.Locker.WithLock(ctx, lockKey, lease, func() error {
		return w.scanOnce(ctx, deps, profile)
	})
}

func (w *SuiWatcher) scanOnce(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	latest, err := w.latestCheckpoint(ctx, profile)
	if err != nil {
		return err
	}
	if deps.Checkpoint != nil {
		_ = deps.Checkpoint.UpdateObserved(ctx, profile.Chain, latest)
	}
	lastConfirmed := int64(0)
	if deps.Checkpoint != nil {
		lastConfirmed, _ = deps.Checkpoint.ConfirmedHeight(ctx, profile.Chain)
	}
	confirmed := latest - maxInt64(profile.ConfirmationDepth, 1)
	if confirmed < 1 {
		return nil
	}
	if lastConfirmed <= 0 {
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, maxInt64(1, confirmed-maxInt64(chainBackfillBlocks(deps, profile), 12)))
		deps.LoggerOrDefault().Info("sui checkpoint initialized", "chain", profile.Chain, "confirmedCheckpoint", confirmed)
		return nil
	}
	for _, rule := range matchingRules(profile, deps.Config.Rules) {
		if !strings.EqualFold(rule.Family, "") && !strings.EqualFold(rule.Family, "SUI") {
			continue
		}
		if err := w.scanRule(ctx, deps, profile, rule, lastConfirmed); err != nil {
			return err
		}
	}
	if deps.Checkpoint != nil && confirmed > lastConfirmed {
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, confirmed)
	}
	return nil
}

func (w *SuiWatcher) latestCheckpoint(ctx context.Context, profile config.ChainProfile) (int64, error) {
	var result string
	if err := w.rpc(ctx, profile, "sui_getLatestCheckpointSequenceNumber", []any{}, &result); err != nil {
		return 0, err
	}
	value, err := strconv.ParseInt(result, 10, 64)
	if err != nil {
		return 0, err
	}
	return value, nil
}

func (w *SuiWatcher) scanRule(ctx context.Context, deps Dependencies, profile config.ChainProfile, rule config.WatchRule, lastConfirmed int64) error {
	destinations := destinationAddresses(ctx, deps, profile.Chain, rule)
	if len(destinations) == 0 {
		return nil
	}
	limit := int(minInt64(maxInt64(chainLogBatchBlocks(deps, profile), 50), 100))
	var result suiEventsResult
	query := map[string]any{"MoveEventType": rule.TokenAddress}
	if err := w.rpc(ctx, profile, "suix_queryEvents", []any{query, nil, limit, false}, &result); err != nil {
		return err
	}
	active := canonicalSet(destinations)
	for _, item := range result.Data {
		if item.ID.TxDigest == "" {
			continue
		}
		to := firstText(jsonText(item.ParsedJSON["recipient"]), jsonText(item.ParsedJSON["to"]), jsonText(item.ParsedJSON["receiver"]), item.Sender)
		if _, ok := active[canonicalAddress(to)]; !ok {
			continue
		}
		eventSeq, _ := strconv.ParseInt(item.ID.EventSeq, 10, 64)
		amount := formatDecimalString(firstText(jsonText(item.ParsedJSON["amount"]), jsonText(item.ParsedJSON["value"])), rule.Decimals)
		event := model.ChainPaymentEvent{
			EventID:            chainEventID("SUI_MOVE_EVENT_SCAN", profile.Chain, item.ID.TxDigest, to, rule.TokenAddress),
			Source:             "SUI_MOVE_EVENT_SCAN",
			Chain:              profile.Chain,
			Token:              defaultToken(rule.Token),
			TokenAddress:       rule.TokenAddress,
			TxHash:             item.ID.TxDigest,
			SourceAddress:      firstText(jsonText(item.ParsedJSON["sender"]), jsonText(item.ParsedJSON["from"]), item.Sender),
			DestinationAddress: to,
			Amount:             amount,
			BlockNumber:        lastConfirmed + 1,
			LogIndex:           eventSeq,
			BlockTimestamp:     millisTime(jsonInt64(item.TimestampMs)),
			ObservedAt:         time.Now().UTC(),
		}
		deps.LoggerOrDefault().Info("sui token transfer published", "chain", event.Chain, "token", event.Token, "txHash", event.TxHash, "destinationAddress", event.DestinationAddress, "amount", event.Amount)
		if deps.Publisher != nil {
			if err := deps.Publisher.Publish(ctx, event); err != nil {
				return err
			}
		}
	}
	return nil
}

func (w *SuiWatcher) rpc(ctx context.Context, profile config.ChainProfile, method string, params []any, out any) error {
	payload, _ := json.Marshal(map[string]any{"jsonrpc": "2.0", "id": 1, "method": method, "params": params})
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, profile.RpcURL, bytes.NewReader(payload))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := w.httpClient().Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return fmt.Errorf("sui rpc status %d", resp.StatusCode)
	}
	var envelope struct {
		Result json.RawMessage `json:"result"`
		Error  any             `json:"error"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&envelope); err != nil {
		return err
	}
	if envelope.Error != nil {
		return fmt.Errorf("sui rpc error: %v", envelope.Error)
	}
	return json.Unmarshal(envelope.Result, out)
}

func (w *SuiWatcher) httpClient() *http.Client {
	if w.client != nil {
		return w.client
	}
	return defaultHTTPClient()
}

type suiEventsResult struct {
	Data []struct {
		ID struct {
			TxDigest string `json:"txDigest"`
			EventSeq string `json:"eventSeq"`
		} `json:"id"`
		Sender      string         `json:"sender"`
		ParsedJSON  map[string]any `json:"parsedJson"`
		TimestampMs any            `json:"timestampMs"`
	} `json:"data"`
}

func jsonText(value any) string {
	switch typed := value.(type) {
	case string:
		return typed
	case float64:
		return strconv.FormatInt(int64(typed), 10)
	case json.Number:
		return typed.String()
	default:
		if value == nil {
			return ""
		}
		return fmt.Sprint(value)
	}
}

func jsonInt64(value any) int64 {
	switch typed := value.(type) {
	case string:
		n, _ := strconv.ParseInt(typed, 10, 64)
		return n
	case float64:
		return int64(typed)
	case json.Number:
		n, _ := typed.Int64()
		return n
	default:
		return 0
	}
}
