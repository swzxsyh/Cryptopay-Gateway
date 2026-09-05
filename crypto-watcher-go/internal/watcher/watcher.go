package watcher

import (
	"context"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/coord"
	"cryptopay-gateway-watcher-go/internal/dispatch"
	"cryptopay-gateway-watcher-go/internal/lock"
	"cryptopay-gateway-watcher-go/internal/store"
	"log/slog"
)

// ChainWatcher is the Go counterpart of the Java watcher strategy interface.
type ChainWatcher interface {
	Name() string
	Supports(profile config.ChainProfile) bool
	Bootstrap(ctx context.Context, deps Dependencies, profile config.ChainProfile) error
	Scan(ctx context.Context, deps Dependencies, profile config.ChainProfile) error
}

// Dependencies are shared runtime services for all watchers.
type Dependencies struct {
	Logger       *slog.Logger
	Publisher    dispatch.Publisher
	SubPublisher dispatch.SubscriptionPublisher
	Checkpoint   store.CheckpointStore
	ActiveDest   store.ActiveDestinationStore
	Leader       coord.LeaderElector
	Locker       lock.Locker
	Config       config.Config
	NodeID       string
}

func (d Dependencies) LoggerOrDefault() *slog.Logger {
	if d.Logger == nil {
		return slog.Default()
	}
	return d.Logger
}
