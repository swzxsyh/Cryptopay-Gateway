package config

import (
	"context"
	"database/sql"
	"fmt"
	"strings"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

// LoadFromDB overlays runtime watcher config from the same normalized tables used by Java.
func LoadFromDB(dsn string, base Config) (Config, error) {
	dsn = strings.TrimSpace(dsn)
	if dsn == "" {
		return base, fmt.Errorf("WATCHER_MYSQL_DSN is empty")
	}
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return base, err
	}
	defer db.Close()
	db.SetMaxOpenConns(4)
	db.SetMaxIdleConns(2)
	db.SetConnMaxLifetime(5 * time.Minute)

	ctx, cancel := context.WithTimeout(context.Background(), 8*time.Second)
	defer cancel()
	if err := db.PingContext(ctx); err != nil {
		return base, err
	}

	global, err := loadGlobalScanner(ctx, db, base)
	if err != nil {
		return base, err
	}
	chains, err := loadChains(ctx, db, global)
	if err != nil {
		return base, err
	}
	rules, err := loadTokenRules(ctx, db)
	if err != nil {
		return base, err
	}
	subscription, err := loadSubscription(ctx, db, base.Subscription)
	if err != nil {
		return base, err
	}
	global.Chains = chains
	global.Rules = rules
	global.Subscription = subscription
	global.Source = "DB"
	return global, nil
}

func loadGlobalScanner(ctx context.Context, db *sql.DB, base Config) (Config, error) {
	row := db.QueryRowContext(ctx, `
select confirmation_depth, scan_interval_seconds, backfill_blocks, log_scan_batch_blocks,
       log_scan_retry_attempts, websocket_enabled, websocket_leader_lease_seconds
from payment_scanner_config
where config_scope = 'GLOBAL'
order by id asc
limit 1`)
	var confirmationDepth, intervalSeconds, backfillBlocks, batchBlocks, retryAttempts, wsLease sql.NullInt64
	var websocketEnabled sql.NullBool
	err := row.Scan(&confirmationDepth, &intervalSeconds, &backfillBlocks, &batchBlocks, &retryAttempts, &websocketEnabled, &wsLease)
	if err != nil && err != sql.ErrNoRows {
		return base, err
	}
	if intervalSeconds.Valid && intervalSeconds.Int64 > 0 {
		base.ScanInterval = time.Duration(intervalSeconds.Int64) * time.Second
	}
	if backfillBlocks.Valid && backfillBlocks.Int64 > 0 {
		base.Scan.BackfillBlocks = backfillBlocks.Int64
	}
	if batchBlocks.Valid && batchBlocks.Int64 > 0 {
		base.Scan.LogBatchBlocks = batchBlocks.Int64
	}
	if retryAttempts.Valid && retryAttempts.Int64 > 0 {
		base.Scan.RetryAttempts = int(retryAttempts.Int64)
	}
	if websocketEnabled.Valid {
		base.Scan.UseWebsocketHeads = websocketEnabled.Bool
	}
	if wsLease.Valid && wsLease.Int64 > 0 {
		base.Leader.LeaseSeconds = int(wsLease.Int64)
	}
	return base, nil
}

func loadChains(ctx context.Context, db *sql.DB, base Config) ([]ChainProfile, error) {
	rows, err := db.QueryContext(ctx, `
select c.chain_code, c.rpc_url, c.ws_url,
       greatest(coalesce(cs.confirmation_depth, 0), coalesce(c.confirmation_depth, 0), coalesce(tm.max_token_confirmation_depth, 0), ?) as confirmation_depth,
       coalesce(cs.scan_interval_seconds, 0) as scan_interval_seconds,
       coalesce(cs.backfill_blocks, 0) as backfill_blocks,
       coalesce(cs.log_scan_batch_blocks, 0) as log_scan_batch_blocks,
       coalesce(cs.log_scan_retry_attempts, 0) as log_scan_retry_attempts,
       cs.websocket_enabled,
       coalesce(cs.websocket_leader_lease_seconds, 0) as websocket_leader_lease_seconds
from payment_chain_config c
left join payment_chain_scanner_config cs
  on cs.chain_code = c.chain_code and cs.enabled = 1
left join (
  select chain_code, max(coalesce(confirmation_depth, 0)) as max_token_confirmation_depth
  from payment_token_config
  where enabled = 1
  group by chain_code
) tm on tm.chain_code = c.chain_code
where c.enabled = 1
order by c.sort_no asc, c.id asc`,
		defaultConfirmationDepth(base))
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []ChainProfile
	for rows.Next() {
		var chain, rpcURL, wsURL string
		var depth, intervalSeconds, backfillBlocks, batchBlocks, retryAttempts, wsLease int64
		var websocketEnabled sql.NullBool
		if err := rows.Scan(&chain, &rpcURL, &wsURL, &depth, &intervalSeconds, &backfillBlocks, &batchBlocks, &retryAttempts, &websocketEnabled, &wsLease); err != nil {
			return nil, err
		}
		chain = strings.ToUpper(strings.TrimSpace(chain))
		if chain == "" {
			continue
		}
		chainScanInterval := base.ScanInterval
		if intervalSeconds > 0 {
			chainScanInterval = time.Duration(intervalSeconds) * time.Second
		}
		chainBackfillBlocks := base.Scan.BackfillBlocks
		if backfillBlocks > 0 {
			chainBackfillBlocks = backfillBlocks
		}
		chainBatchBlocks := base.Scan.LogBatchBlocks
		if batchBlocks > 0 {
			chainBatchBlocks = batchBlocks
		}
		chainRetryAttempts := base.Scan.RetryAttempts
		if retryAttempts > 0 {
			chainRetryAttempts = int(retryAttempts)
		}
		chainLeaderLease := time.Duration(base.Leader.LeaseSeconds) * time.Second
		if wsLease > 0 {
			chainLeaderLease = time.Duration(wsLease) * time.Second
		}
		chainWebsocketEnabled := base.Scan.UseWebsocketHeads
		if websocketEnabled.Valid {
			chainWebsocketEnabled = websocketEnabled.Bool
		}
		out = append(out, ChainProfile{
			Chain:             chain,
			Family:            guessFamily(chain),
			Enabled:           true,
			RpcURL:            strings.TrimSpace(rpcURL),
			WsURL:             strings.TrimSpace(wsURL),
			ConfirmationDepth: depth,
			ScanInterval:      chainScanInterval,
			BackfillBlocks:    chainBackfillBlocks,
			LogBatchBlocks:    chainBatchBlocks,
			RetryAttempts:     chainRetryAttempts,
			WebsocketEnabled:  chainWebsocketEnabled,
			WebsocketOverride: websocketEnabled.Valid,
			LeaderLease:       chainLeaderLease,
		})
	}
	return out, rows.Err()
}

func loadTokenRules(ctx context.Context, db *sql.DB) ([]WatchRule, error) {
	rows, err := db.QueryContext(ctx, `
select t.chain_code, t.token_symbol, t.token_address, t.decimals, coalesce(t.confirmation_depth, 0)
from payment_token_config t
join payment_chain_config c on c.chain_code = t.chain_code and c.enabled = 1
where t.enabled = 1 and t.token_address is not null and t.token_address <> ''
order by c.sort_no asc, t.sort_no asc, t.id asc`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []WatchRule
	for rows.Next() {
		var chain, token, tokenAddress string
		var decimals, tokenDepth int
		if err := rows.Scan(&chain, &token, &tokenAddress, &decimals, &tokenDepth); err != nil {
			return nil, err
		}
		chain = strings.ToUpper(strings.TrimSpace(chain))
		out = append(out, WatchRule{
			Chain:        chain,
			Family:       guessFamily(chain),
			Token:        strings.TrimSpace(token),
			TokenAddress: strings.TrimSpace(tokenAddress),
			Decimals:     decimals,
			Enabled:      true,
		})
	}
	return out, rows.Err()
}

func loadSubscription(ctx context.Context, db *sql.DB, base SubscriptionConfig) (SubscriptionConfig, error) {
	row := db.QueryRowContext(ctx, `
select subscription_enabled, erc1337_executor_address, superfluid_cfa_address, superfluid_host_address
from payment_subscription_config
order by id asc
limit 1`)
	var enabled sql.NullBool
	var erc1337, cfa, host sql.NullString
	err := row.Scan(&enabled, &erc1337, &cfa, &host)
	if err != nil && err != sql.ErrNoRows {
		return base, err
	}
	if enabled.Valid {
		base.Enabled = enabled.Bool
	}
	contracts := make([]string, 0, 3)
	for _, value := range []sql.NullString{erc1337, cfa, host} {
		if value.Valid && strings.TrimSpace(value.String) != "" {
			contracts = append(contracts, strings.TrimSpace(value.String))
		}
	}
	if len(contracts) > 0 {
		base.Contracts = contracts
	}
	return base, nil
}

func defaultConfirmationDepth(base Config) int64 {
	if len(base.Chains) > 0 && base.Chains[0].ConfirmationDepth > 0 {
		return base.Chains[0].ConfirmationDepth
	}
	return 12
}

func boolToInt(value bool) int {
	if value {
		return 1
	}
	return 0
}
