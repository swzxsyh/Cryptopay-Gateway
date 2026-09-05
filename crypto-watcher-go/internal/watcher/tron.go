package watcher

import (
	"context"
	"encoding/json"
	"fmt"
	"math/big"
	"net/http"
	"net/url"
	"strings"
	"sync"
	"time"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/model"
)

// TronWatcher scans TRC20 Transfer events through a TronGrid-compatible event API.
type TronWatcher struct {
	mu      sync.Mutex
	started map[string]struct{}
	client  *http.Client
}

func (w *TronWatcher) Name() string { return "tron" }

func (w *TronWatcher) Supports(profile config.ChainProfile) bool {
	return strings.EqualFold(profile.Family, "TRON")
}

func (w *TronWatcher) Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	w.mu.Lock()
	if w.started == nil {
		w.started = make(map[string]struct{})
	}
	if w.client == nil {
		w.client = &http.Client{Timeout: 12 * time.Second}
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

func (w *TronWatcher) Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	return w.scanWithLock(ctx, deps, profile)
}

func (w *TronWatcher) run(ctx context.Context, deps Dependencies, profile config.ChainProfile) {
	logger := deps.LoggerOrDefault()
	if strings.TrimSpace(profile.RpcURL) == "" {
		logger.Warn("tron watcher has no event api endpoint", "chain", profile.Chain)
		return
	}
	interval := chainScanInterval(deps, profile)
	if interval <= 0 {
		interval = 15 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	logger.Info("tron watcher started", "chain", profile.Chain, "eventApi", profile.RpcURL)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if err := w.scanWithLock(ctx, deps, profile); err != nil {
				logger.Warn("tron scan failed", "chain", profile.Chain, "error", err)
			}
		}
	}
}

func (w *TronWatcher) scanWithLock(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	lease := time.Duration(maxInt(deps.Config.Lock.LeaseSeconds, 10)) * time.Second
	lockKey := deps.Config.Redis.Prefix + ":scanner:tron:" + strings.ToUpper(strings.TrimSpace(profile.Chain))
	if deps.Locker == nil {
		return w.scanOnce(ctx, deps, profile)
	}
	return deps.Locker.WithLock(ctx, lockKey, lease, func() error {
		return w.scanOnce(ctx, deps, profile)
	})
}

func (w *TronWatcher) scanOnce(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	rules := matchingRules(profile, deps.Config.Rules)
	if len(rules) == 0 {
		deps.LoggerOrDefault().Debug("tron watcher has no active rules", "chain", profile.Chain)
		return nil
	}
	latest, err := w.latestBlock(ctx, profile)
	if err == nil && deps.Checkpoint != nil {
		_ = deps.Checkpoint.UpdateObserved(ctx, profile.Chain, latest)
	}
	lastConfirmed := int64(0)
	if deps.Checkpoint != nil {
		lastConfirmed, _ = deps.Checkpoint.ConfirmedHeight(ctx, profile.Chain)
	}
	if lastConfirmed <= 0 && latest > 0 {
		startAt := maxInt64(1, latest-maxInt64(chainBackfillBlocks(deps, profile), 20))
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, startAt)
		deps.LoggerOrDefault().Info("tron checkpoint initialized", "chain", profile.Chain, "confirmedBlock", startAt)
		return nil
	}
	maxSeen := lastConfirmed
	for _, rule := range rules {
		if !strings.EqualFold(rule.Family, "") && !strings.EqualFold(rule.Family, "TRON") {
			continue
		}
		seen, err := w.scanRule(ctx, deps, profile, rule, lastConfirmed)
		if err != nil {
			return err
		}
		if seen > maxSeen {
			maxSeen = seen
		}
	}
	if deps.Checkpoint != nil && maxSeen > lastConfirmed {
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, maxSeen)
	}
	return nil
}

func (w *TronWatcher) latestBlock(ctx context.Context, profile config.ChainProfile) (int64, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, strings.TrimRight(profile.RpcURL, "/")+"/walletsolidity/getnowblock", nil)
	if err != nil {
		return 0, err
	}
	req.Header.Set("Content-Type", "application/json")
	addTronAPIKey(req)
	resp, err := w.httpClient().Do(req)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()
	var body struct {
		BlockHeader struct {
			RawData struct {
				Number int64 `json:"number"`
			} `json:"raw_data"`
		} `json:"block_header"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return 0, err
	}
	return body.BlockHeader.RawData.Number, nil
}

func (w *TronWatcher) scanRule(ctx context.Context, deps Dependencies, profile config.ChainProfile, rule config.WatchRule, lastConfirmed int64) (int64, error) {
	destinations := destinationAddresses(ctx, deps, profile.Chain, rule)
	if len(destinations) == 0 {
		deps.LoggerOrDefault().Debug("tron scan skipped because no active destination exists", "chain", profile.Chain, "tokenAddress", rule.TokenAddress)
		return lastConfirmed, nil
	}
	endpoint := fmt.Sprintf("%s/v1/contracts/%s/events", strings.TrimRight(profile.RpcURL, "/"), url.PathEscape(rule.TokenAddress))
	values := url.Values{}
	values.Set("event_name", "Transfer")
	values.Set("only_confirmed", "true")
	values.Set("limit", fmt.Sprintf("%d", minInt64(maxInt64(chainLogBatchBlocks(deps, profile), 50), 200)))
	values.Set("order_by", "block_timestamp,asc")
	if lastConfirmed > 0 {
		values.Set("min_block_number", fmt.Sprintf("%d", maxInt64(1, lastConfirmed+1-maxInt64(chainBackfillBlocks(deps, profile), 20))))
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint+"?"+values.Encode(), nil)
	if err != nil {
		return lastConfirmed, err
	}
	addTronAPIKey(req)
	resp, err := w.httpClient().Do(req)
	if err != nil {
		return lastConfirmed, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return lastConfirmed, fmt.Errorf("tron event api status %d", resp.StatusCode)
	}
	var body tronEventsResponse
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return lastConfirmed, err
	}
	active := canonicalSet(destinations)
	maxSeen := lastConfirmed
	for _, item := range body.Data {
		if item.BlockNumber > 0 && item.BlockNumber > maxSeen {
			maxSeen = item.BlockNumber
		}
		to := firstText(item.Result.To, item.Result.Recipient)
		if _, ok := active[canonicalAddress(to)]; !ok {
			continue
		}
		event := model.ChainPaymentEvent{
			EventID:            chainEventID("TRON_TRC20_EVENT_API", profile.Chain, item.TransactionID, to, rule.TokenAddress),
			Source:             "TRON_TRC20_EVENT_API",
			Chain:              profile.Chain,
			Token:              defaultToken(rule.Token),
			TokenAddress:       rule.TokenAddress,
			TxHash:             item.TransactionID,
			SourceAddress:      firstText(item.Result.From, item.Result.Owner),
			DestinationAddress: to,
			Amount:             formatDecimalString(firstText(item.Result.Value, item.Result.Amount), rule.Decimals),
			BlockNumber:        item.BlockNumber,
			LogIndex:           item.EventIndex,
			BlockTimestamp:     millisTime(item.BlockTimestamp),
			ObservedAt:         time.Now().UTC(),
		}
		deps.LoggerOrDefault().Info("tron trc20 transfer published", "chain", event.Chain, "token", event.Token, "txHash", event.TxHash, "destinationAddress", event.DestinationAddress, "amount", event.Amount, "blockNumber", event.BlockNumber)
		if deps.Publisher != nil {
			if err := deps.Publisher.Publish(ctx, event); err != nil {
				return maxSeen, err
			}
		}
	}
	return maxSeen, nil
}

type tronEventsResponse struct {
	Data []struct {
		TransactionID  string `json:"transaction_id"`
		BlockNumber    int64  `json:"block_number"`
		BlockTimestamp int64  `json:"block_timestamp"`
		EventIndex     int64  `json:"event_index"`
		Result         struct {
			From      string `json:"from"`
			Owner     string `json:"owner"`
			To        string `json:"to"`
			Recipient string `json:"recipient"`
			Value     string `json:"value"`
			Amount    string `json:"amount"`
		} `json:"result"`
	} `json:"data"`
}

func addTronAPIKey(req *http.Request) {
	if key := strings.TrimSpace(getenv("WATCHER_TRON_API_KEY")); key != "" {
		req.Header.Set("TRON-PRO-API-KEY", key)
	}
}

func (w *TronWatcher) httpClient() *http.Client {
	if w.client != nil {
		return w.client
	}
	return &http.Client{Timeout: 12 * time.Second}
}

func formatDecimalString(raw string, decimals int) string {
	value := new(big.Int)
	if _, ok := value.SetString(strings.TrimSpace(raw), 10); !ok {
		return "0"
	}
	if decimals <= 0 {
		return value.String()
	}
	denom := new(big.Int).Exp(big.NewInt(10), big.NewInt(int64(decimals)), nil)
	return new(big.Rat).SetFrac(value, denom).FloatString(decimals)
}
