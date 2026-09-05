package config

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

// Config is the Go watcher runtime config.
type Config struct {
	ScanInterval time.Duration
	Chains       []ChainProfile
	Rules        []WatchRule
	Subscription SubscriptionConfig
	Redis        RedisConfig
	Leader       LeaderConfig
	Lock         LockConfig
	Scan         ScanConfig
	Messaging    MessagingConfig
	Source       string
	MySQLDSN     string
}

// ChainProfile is a normalized chain entry.
type ChainProfile struct {
	Chain             string
	Family            string
	Enabled           bool
	RpcURL            string
	WsURL             string
	ConfirmationDepth int64
	ScanInterval      time.Duration
	BackfillBlocks    int64
	LogBatchBlocks    int64
	RetryAttempts     int
	WebsocketEnabled  bool
	WebsocketOverride bool
	LeaderLease       time.Duration
}

type RedisConfig struct {
	Addr     string
	Password string
	DB       int
	Prefix   string
}

type LeaderConfig struct {
	LeaseSeconds int
}

type LockConfig struct {
	LeaseSeconds int
}

type ScanConfig struct {
	BackfillBlocks    int64
	LogBatchBlocks    int64
	RetryAttempts     int
	UseWebsocketHeads bool
}

type MessagingConfig struct {
	ChainPaymentTopic      string
	SubscriptionEventTopic string
}

type SubscriptionConfig struct {
	Enabled   bool
	Contracts []string
}

type WatchRule struct {
	Chain        string   `json:"chain"`
	Family       string   `json:"family"`
	Token        string   `json:"token"`
	TokenAddress string   `json:"tokenAddress"`
	Destinations []string `json:"destinations"`
	Decimals     int      `json:"decimals"`
	Enabled      bool     `json:"enabled"`
}

// LoadFromEnv loads a minimal config from env.
// This is intentionally lightweight; later we can wire YAML or DB config.
func LoadFromEnv() Config {
	interval := time.Second * 15
	if v := strings.TrimSpace(os.Getenv("WATCHER_SCAN_INTERVAL_SECONDS")); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n > 0 {
			interval = time.Duration(n) * time.Second
		}
	}

	chains := parseChains(os.Getenv("WATCHER_CHAINS"))
	rules := parseRules(os.Getenv("WATCHER_RULES_JSON"))
	redisDB := 0
	if v := strings.TrimSpace(os.Getenv("WATCHER_REDIS_DB")); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n >= 0 {
			redisDB = n
		}
	}
	leaderLease := envInt("WATCHER_LEADER_LEASE_SECONDS", 30)
	lockLease := envInt("WATCHER_LOCK_LEASE_SECONDS", 10)
	backfillBlocks := envInt64("WATCHER_BACKFILL_BLOCKS", 12)
	logBatchBlocks := envInt64("WATCHER_LOG_BATCH_BLOCKS", 50)
	retryAttempts := envInt("WATCHER_LOG_RETRY_ATTEMPTS", 3)
	cfg := Config{
		ScanInterval: interval,
		Chains:       chains,
		Rules:        rules,
		Source:       strings.ToUpper(defaultString(os.Getenv("WATCHER_CONFIG_SOURCE"), "ENV")),
		MySQLDSN:     strings.TrimSpace(os.Getenv("WATCHER_MYSQL_DSN")),
		Redis: RedisConfig{
			Addr:     strings.TrimSpace(os.Getenv("WATCHER_REDIS_ADDR")),
			Password: strings.TrimSpace(os.Getenv("WATCHER_REDIS_PASSWORD")),
			DB:       redisDB,
			Prefix:   defaultString(os.Getenv("WATCHER_REDIS_PREFIX"), "crypto:payment"),
		},
		Leader: LeaderConfig{LeaseSeconds: leaderLease},
		Lock:   LockConfig{LeaseSeconds: lockLease},
		Scan: ScanConfig{
			BackfillBlocks:    backfillBlocks,
			LogBatchBlocks:    logBatchBlocks,
			RetryAttempts:     retryAttempts,
			UseWebsocketHeads: strings.EqualFold(strings.TrimSpace(os.Getenv("WATCHER_USE_WEBSOCKET_HEADS")), "true"),
		},
		Messaging: MessagingConfig{
			ChainPaymentTopic:      defaultString(os.Getenv("WATCHER_CHAIN_PAYMENT_TOPIC"), "crypto:payment:chain-payment-events"),
			SubscriptionEventTopic: defaultString(os.Getenv("WATCHER_SUBSCRIPTION_EVENT_TOPIC"), "crypto:payment:subscription-events"),
		},
		Subscription: SubscriptionConfig{
			Enabled:   !strings.EqualFold(strings.TrimSpace(os.Getenv("WATCHER_SUBSCRIPTION_ENABLED")), "false"),
			Contracts: splitCSV(os.Getenv("WATCHER_SUBSCRIPTION_CONTRACTS")),
		},
	}
	if cfg.Source == "DB" {
		if dbCfg, err := LoadFromDB(cfg.MySQLDSN, cfg); err == nil {
			return dbCfg
		} else {
			fmt.Fprintf(os.Stderr, "load watcher config from db failed: %v; fallback to env config\n", err)
		}
	}
	return cfg
}

func splitCSV(raw string) []string {
	parts := strings.Split(strings.TrimSpace(raw), ",")
	out := make([]string, 0, len(parts))
	for _, part := range parts {
		value := strings.TrimSpace(part)
		if value != "" {
			out = append(out, value)
		}
	}
	return out
}

func parseChains(raw string) []ChainProfile {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return parseChainsJSON(os.Getenv("WATCHER_CHAINS_JSON"))
	}
	parts := strings.Split(raw, ",")
	out := make([]ChainProfile, 0, len(parts))
	for _, part := range parts {
		value := strings.TrimSpace(part)
		if value == "" {
			continue
		}
		out = append(out, ChainProfile{
			Chain:             strings.ToUpper(value),
			Family:            chainEnv(value, "FAMILY", guessFamily(value)),
			Enabled:           !strings.EqualFold(chainEnv(value, "ENABLED", "true"), "false"),
			RpcURL:            chainEnv(value, "RPC_URL", ""),
			WsURL:             chainEnv(value, "WS_URL", ""),
			ConfirmationDepth: chainEnvInt64(value, "CONFIRMATION_DEPTH", 12),
		})
	}
	return out
}

func parseChainsJSON(raw string) []ChainProfile {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return []ChainProfile{}
	}
	type chainProfileJSON struct {
		Chain             string `json:"chain"`
		Family            string `json:"family"`
		Enabled           *bool  `json:"enabled"`
		RpcURL            string `json:"rpcUrl"`
		WsURL             string `json:"wsUrl"`
		ConfirmationDepth int64  `json:"confirmationDepth"`
	}
	var chains []chainProfileJSON
	if err := json.Unmarshal([]byte(raw), &chains); err != nil {
		return []ChainProfile{}
	}
	out := make([]ChainProfile, 0, len(chains))
	for _, chain := range chains {
		chainCode := strings.ToUpper(strings.TrimSpace(chain.Chain))
		if chainCode == "" {
			continue
		}
		family := strings.ToUpper(strings.TrimSpace(chain.Family))
		if family == "" {
			family = guessFamily(chainCode)
		}
		enabled := true
		if chain.Enabled != nil {
			enabled = *chain.Enabled
		}
		depth := chain.ConfirmationDepth
		if depth <= 0 {
			depth = 12
		}
		out = append(out, ChainProfile{
			Chain:             chainCode,
			Family:            family,
			Enabled:           enabled,
			RpcURL:            strings.TrimSpace(chain.RpcURL),
			WsURL:             strings.TrimSpace(chain.WsURL),
			ConfirmationDepth: depth,
		})
	}
	return out
}

func guessFamily(chain string) string {
	upper := strings.ToUpper(strings.TrimSpace(chain))
	switch {
	case strings.HasPrefix(upper, "SOL"):
		return "SOLANA"
	case strings.HasPrefix(upper, "TON"):
		return "TON"
	case strings.HasPrefix(upper, "SUI"):
		return "SUI"
	case strings.HasPrefix(upper, "TRON"):
		return "TRON"
	default:
		return "EVM"
	}
}

func envInt(key string, defaultValue int) int {
	if v := strings.TrimSpace(os.Getenv(key)); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n > 0 {
			return n
		}
	}
	return defaultValue
}

func defaultString(value string, fallback string) string {
	value = strings.TrimSpace(value)
	if value == "" {
		return fallback
	}
	return value
}

func (r RedisConfig) String() string {
	return fmt.Sprintf("%s/%d", r.Addr, r.DB)
}

func parseRules(raw string) []WatchRule {
	raw = strings.TrimSpace(raw)
	if raw == "" {
		return []WatchRule{}
	}
	type watchRuleJSON struct {
		Chain        string   `json:"chain"`
		Family       string   `json:"family"`
		Token        string   `json:"token"`
		TokenAddress string   `json:"tokenAddress"`
		Destinations []string `json:"destinations"`
		Decimals     int      `json:"decimals"`
		Enabled      *bool    `json:"enabled"`
	}
	var rules []watchRuleJSON
	if err := json.Unmarshal([]byte(raw), &rules); err != nil {
		return []WatchRule{}
	}
	out := make([]WatchRule, 0, len(rules))
	for _, rule := range rules {
		if strings.TrimSpace(rule.Chain) == "" || strings.TrimSpace(rule.TokenAddress) == "" {
			continue
		}
		enabled := true
		if rule.Enabled != nil {
			enabled = *rule.Enabled
		}
		family := strings.ToUpper(strings.TrimSpace(rule.Family))
		if family == "" {
			family = guessFamily(rule.Chain)
		}
		out = append(out, WatchRule{
			Chain:        strings.ToUpper(strings.TrimSpace(rule.Chain)),
			Family:       family,
			Token:        strings.TrimSpace(rule.Token),
			TokenAddress: strings.TrimSpace(rule.TokenAddress),
			Destinations: rule.Destinations,
			Decimals:     rule.Decimals,
			Enabled:      enabled,
		})
	}
	return out
}

func envInt64(key string, defaultValue int64) int64 {
	if v := strings.TrimSpace(os.Getenv(key)); v != "" {
		if n, err := strconv.ParseInt(v, 10, 64); err == nil && n > 0 {
			return n
		}
	}
	return defaultValue
}

func chainEnv(chain, suffix, fallback string) string {
	key := "WATCHER_CHAIN_" + envName(chain) + "_" + suffix
	return defaultString(os.Getenv(key), fallback)
}

func chainEnvInt64(chain, suffix string, fallback int64) int64 {
	return envInt64("WATCHER_CHAIN_"+envName(chain)+"_"+suffix, fallback)
}

func envName(value string) string {
	replacer := strings.NewReplacer("-", "_", " ", "_", ".", "_")
	return strings.ToUpper(replacer.Replace(strings.TrimSpace(value)))
}
