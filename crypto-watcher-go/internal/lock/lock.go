package lock

import (
	"context"
	"time"
)

// Locker performs distributed mutual exclusion.
type Locker interface {
	WithLock(ctx context.Context, key string, ttl time.Duration, fn func() error) error
}
