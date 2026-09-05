package coord

import (
	"context"
	"time"
)

// LeaderElector manages per-chain watcher leadership.
type LeaderElector interface {
	TryAcquire(ctx context.Context, chain string, ttl time.Duration) (bool, error)
	Renew(ctx context.Context, chain string, ttl time.Duration) (bool, error)
	Release(ctx context.Context, chain string) error
	Current(ctx context.Context, chain string) (string, error)
}
