package watcher

import (
	"time"

	"cryptopay-gateway-watcher-go/internal/config"
)

func chainScanInterval(deps Dependencies, profile config.ChainProfile) time.Duration {
	if profile.ScanInterval > 0 {
		return profile.ScanInterval
	}
	return deps.Config.ScanInterval
}

func chainBackfillBlocks(deps Dependencies, profile config.ChainProfile) int64 {
	if profile.BackfillBlocks > 0 {
		return profile.BackfillBlocks
	}
	return deps.Config.Scan.BackfillBlocks
}

func chainLogBatchBlocks(deps Dependencies, profile config.ChainProfile) int64 {
	if profile.LogBatchBlocks > 0 {
		return profile.LogBatchBlocks
	}
	return deps.Config.Scan.LogBatchBlocks
}

func chainRetryAttempts(deps Dependencies, profile config.ChainProfile) int {
	if profile.RetryAttempts > 0 {
		return profile.RetryAttempts
	}
	return deps.Config.Scan.RetryAttempts
}

func chainWebsocketEnabled(deps Dependencies, profile config.ChainProfile) bool {
	if profile.WebsocketOverride {
		return profile.WebsocketEnabled
	}
	return deps.Config.Scan.UseWebsocketHeads
}

func chainLeaderLease(deps Dependencies, profile config.ChainProfile) time.Duration {
	if profile.LeaderLease > 0 {
		return profile.LeaderLease
	}
	return time.Duration(maxInt(deps.Config.Leader.LeaseSeconds, 30)) * time.Second
}
