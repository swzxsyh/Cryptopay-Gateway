package watcher

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/model"
)

// TonWatcher scans TON/Jetton transfers through a TonCenter-compatible HTTP API.
type TonWatcher struct {
	mu      sync.Mutex
	started map[string]struct{}
	client  *http.Client
}

func (w *TonWatcher) Name() string { return "ton" }

func (w *TonWatcher) Supports(profile config.ChainProfile) bool {
	return strings.EqualFold(profile.Family, "TON")
}

func (w *TonWatcher) Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
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

func (w *TonWatcher) Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	return w.scanWithLock(ctx, deps, profile)
}

func (w *TonWatcher) run(ctx context.Context, deps Dependencies, profile config.ChainProfile) {
	logger := deps.LoggerOrDefault()
	if strings.TrimSpace(profile.RpcURL) == "" {
		logger.Warn("ton watcher has no indexer endpoint", "chain", profile.Chain)
		return
	}
	interval := chainScanInterval(deps, profile)
	if interval <= 0 {
		interval = 20 * time.Second
	}
	ticker := time.NewTicker(interval)
	defer ticker.Stop()
	logger.Info("ton watcher started", "chain", profile.Chain, "indexer", profile.RpcURL)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			if err := w.scanWithLock(ctx, deps, profile); err != nil {
				logger.Warn("ton scan failed", "chain", profile.Chain, "error", err)
			}
		}
	}
}

func (w *TonWatcher) scanWithLock(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	lease := time.Duration(maxInt(deps.Config.Lock.LeaseSeconds, 10)) * time.Second
	lockKey := deps.Config.Redis.Prefix + ":scanner:ton:" + strings.ToUpper(strings.TrimSpace(profile.Chain))
	if deps.Locker == nil {
		return w.scanOnce(ctx, deps, profile)
	}
	return deps.Locker.WithLock(ctx, lockKey, lease, func() error {
		return w.scanOnce(ctx, deps, profile)
	})
}

func (w *TonWatcher) scanOnce(ctx context.Context, deps Dependencies, profile config.ChainProfile) error {
	latest, err := w.latestSeqno(ctx, profile)
	if err == nil && deps.Checkpoint != nil {
		_ = deps.Checkpoint.UpdateObserved(ctx, profile.Chain, latest)
	}
	lastConfirmed := int64(0)
	if deps.Checkpoint != nil {
		lastConfirmed, _ = deps.Checkpoint.ConfirmedHeight(ctx, profile.Chain)
	}
	if lastConfirmed <= 0 && latest > 0 {
		_ = deps.Checkpoint.UpdateConfirmed(ctx, profile.Chain, maxInt64(1, latest-maxInt64(chainBackfillBlocks(deps, profile), 20)))
		deps.LoggerOrDefault().Info("ton checkpoint initialized", "chain", profile.Chain, "seqno", latest)
		return nil
	}
	maxSeen := lastConfirmed
	for _, rule := range matchingRules(profile, deps.Config.Rules) {
		if !strings.EqualFold(rule.Family, "") && !strings.EqualFold(rule.Family, "TON") {
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

func (w *TonWatcher) latestSeqno(ctx context.Context, profile config.ChainProfile) (int64, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, strings.TrimRight(profile.RpcURL, "/")+"/getMasterchainInfo", nil)
	if err != nil {
		return 0, err
	}
	addTonAPIKey(req)
	resp, err := w.httpClient().Do(req)
	if err != nil {
		return 0, err
	}
	defer resp.Body.Close()
	var body struct {
		OK     bool `json:"ok"`
		Result struct {
			Last struct {
				Seqno int64 `json:"seqno"`
			} `json:"last"`
		} `json:"result"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return 0, err
	}
	return body.Result.Last.Seqno, nil
}

func (w *TonWatcher) scanRule(ctx context.Context, deps Dependencies, profile config.ChainProfile, rule config.WatchRule, lastConfirmed int64) (int64, error) {
	destinations := destinationAddresses(ctx, deps, profile.Chain, rule)
	if len(destinations) == 0 {
		return lastConfirmed, nil
	}
	maxSeen := lastConfirmed
	for _, destination := range destinations {
		events, err := w.loadAddressTransactions(ctx, profile, destination, int(minInt64(maxInt64(chainLogBatchBlocks(deps, profile), 50), 100)))
		if err != nil {
			return maxSeen, err
		}
		for _, tx := range events {
			if tx.Seqno > 0 && tx.Seqno > maxSeen {
				maxSeen = tx.Seqno
			}
			if tx.Seqno <= lastConfirmed {
				continue
			}
			to := firstText(tx.To, tx.Destination, destination)
			if canonicalAddress(to) != canonicalAddress(destination) {
				continue
			}
			if rule.TokenAddress != "" && tx.TokenAddress != "" && !strings.EqualFold(tx.TokenAddress, rule.TokenAddress) {
				continue
			}
			event := model.ChainPaymentEvent{
				EventID:            chainEventID("TON_INDEXER_SCAN", profile.Chain, tx.Hash, to, rule.TokenAddress),
				Source:             "TON_INDEXER_SCAN",
				Chain:              profile.Chain,
				Token:              defaultToken(rule.Token),
				TokenAddress:       rule.TokenAddress,
				TxHash:             tx.Hash,
				SourceAddress:      tx.From,
				DestinationAddress: to,
				Amount:             formatDecimalString(tx.Amount, rule.Decimals),
				BlockNumber:        tx.Seqno,
				LogIndex:           0,
				BlockTimestamp:     millisTime(tx.Timestamp),
				ObservedAt:         time.Now().UTC(),
			}
			deps.LoggerOrDefault().Info("ton transfer published", "chain", event.Chain, "token", event.Token, "txHash", event.TxHash, "destinationAddress", event.DestinationAddress, "amount", event.Amount, "seqno", event.BlockNumber)
			if deps.Publisher != nil {
				if err := deps.Publisher.Publish(ctx, event); err != nil {
					return maxSeen, err
				}
			}
		}
	}
	return maxSeen, nil
}

func (w *TonWatcher) loadAddressTransactions(ctx context.Context, profile config.ChainProfile, address string, limit int) ([]tonTransferLike, error) {
	base := strings.TrimRight(profile.RpcURL, "/")
	if strings.Contains(strings.ToLower(base), "jetton") {
		return w.loadJettonTransfers(ctx, profile, address, limit)
	}
	values := url.Values{}
	values.Set("address", address)
	values.Set("limit", strconv.Itoa(limit))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, base+"/getTransactions?"+values.Encode(), nil)
	if err != nil {
		return nil, err
	}
	addTonAPIKey(req)
	resp, err := w.httpClient().Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	if resp.StatusCode >= 300 {
		return nil, fmt.Errorf("ton transactions api status %d", resp.StatusCode)
	}
	var body tonTransactionsResponse
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return nil, err
	}
	out := make([]tonTransferLike, 0, len(body.Result))
	for _, item := range body.Result {
		hash := firstText(item.Hash, item.TransactionID)
		out = append(out, tonTransferLike{
			Hash:        hash,
			From:        item.InMsg.Source,
			To:          item.InMsg.Destination,
			Destination: item.InMsg.Destination,
			Amount:      item.InMsg.Value,
			Timestamp:   item.UTxTime,
			Seqno:       item.UTxTime,
		})
	}
	return out, nil
}

func (w *TonWatcher) loadJettonTransfers(ctx context.Context, profile config.ChainProfile, address string, limit int) ([]tonTransferLike, error) {
	values := url.Values{}
	values.Set("account", address)
	values.Set("limit", strconv.Itoa(limit))
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, strings.TrimRight(profile.RpcURL, "/")+"?"+values.Encode(), nil)
	if err != nil {
		return nil, err
	}
	addTonAPIKey(req)
	resp, err := w.httpClient().Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()
	var body struct {
		Transfers []tonTransferLike `json:"transfers"`
		Events    []tonTransferLike `json:"events"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&body); err != nil {
		return nil, err
	}
	if len(body.Transfers) > 0 {
		return body.Transfers, nil
	}
	return body.Events, nil
}

func addTonAPIKey(req *http.Request) {
	if key := strings.TrimSpace(getenv("WATCHER_TON_API_KEY")); key != "" {
		req.Header.Set("X-API-Key", key)
	}
}

func (w *TonWatcher) httpClient() *http.Client {
	if w.client != nil {
		return w.client
	}
	return defaultHTTPClient()
}

type tonTransferLike struct {
	Hash         string `json:"hash"`
	From         string `json:"from"`
	To           string `json:"to"`
	Destination  string `json:"destination"`
	Amount       string `json:"amount"`
	TokenAddress string `json:"tokenAddress"`
	Timestamp    int64  `json:"timestamp"`
	Seqno        int64  `json:"seqno"`
}

type tonTransactionsResponse struct {
	Result []tonTransaction `json:"result"`
}

type tonTransaction struct {
	Hash          string `json:"hash"`
	TransactionID string `json:"transaction_id"`
	UTxTime       int64  `json:"utime"`
	InMsg         struct {
		Source      string `json:"source"`
		Destination string `json:"destination"`
		Value       string `json:"value"`
	} `json:"in_msg"`
}
