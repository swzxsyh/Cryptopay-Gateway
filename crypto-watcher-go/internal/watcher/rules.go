package watcher

import (
	"context"
	"strings"

	"cryptopay-gateway-watcher-go/internal/config"
)

func matchingRules(profile config.ChainProfile, rules []config.WatchRule) []config.WatchRule {
	out := make([]config.WatchRule, 0, len(rules))
	for _, rule := range rules {
		if !rule.Enabled {
			continue
		}
		if !matchesChain(profile, rule) {
			continue
		}
		out = append(out, rule)
	}
	return out
}

func matchesChain(profile config.ChainProfile, rule config.WatchRule) bool {
	if strings.TrimSpace(rule.Chain) == "" {
		return false
	}
	if !strings.EqualFold(profile.Chain, rule.Chain) {
		return false
	}
	if strings.TrimSpace(rule.Family) == "" {
		return true
	}
	return strings.EqualFold(profile.Family, rule.Family)
}

func cleanAddress(value string) string {
	return strings.TrimSpace(strings.ToLower(value))
}

func destinationAddresses(ctx context.Context, deps Dependencies, chain string, rule config.WatchRule) []string {
	if len(rule.Destinations) > 0 {
		return rule.Destinations
	}
	if deps.ActiveDest == nil {
		return nil
	}
	destinations, err := deps.ActiveDest.Members(ctx, chain, rule.TokenAddress)
	if err != nil {
		deps.LoggerOrDefault().Warn("active destination lookup failed", "chain", chain, "tokenAddress", rule.TokenAddress, "error", err)
		return nil
	}
	return destinations
}
