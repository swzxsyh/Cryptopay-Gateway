package app

import (
	"context"
	"errors"
	"log/slog"
	"os"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/coord"
	"cryptopay-gateway-watcher-go/internal/dispatch"
	"cryptopay-gateway-watcher-go/internal/lock"
	"cryptopay-gateway-watcher-go/internal/store"
	"cryptopay-gateway-watcher-go/internal/watcher"

	"github.com/redis/go-redis/v9"
)

// Application wires the watcher runtime.
type Application struct {
	logger *slog.Logger
}

func New(logger *slog.Logger) *Application {
	if logger == nil {
		logger = slog.Default()
	}
	return &Application{logger: logger}
}

func (a *Application) Run(ctx context.Context) error {
	cfg := config.LoadFromEnv()
	if len(cfg.Chains) == 0 {
		a.logger.Warn("no watcher chain configured")
	}

	redisClient := redis.NewClient(&redis.Options{
		Addr:     cfg.Redis.Addr,
		Password: cfg.Redis.Password,
		DB:       cfg.Redis.DB,
	})
	if cfg.Redis.Addr == "" {
		a.logger.Warn("WATCHER_REDIS_ADDR is empty, fallback to in-memory publisher/checkpoint only")
	}

	var publisher dispatch.Publisher = dispatch.NewMemoryPublisher(a.logger)
	var subscriptionPublisher dispatch.SubscriptionPublisher = dispatch.NoopSubscriptionPublisher{}
	var checkpoints store.CheckpointStore = store.NewMemoryCheckpointStore()
	var activeDestinations store.ActiveDestinationStore = store.NewMemoryActiveDestinationStore()
	var leader coord.LeaderElector
	var locker lock.Locker
	if cfg.Redis.Addr != "" {
		publisher = dispatch.NewRedisStreamPublisher(redisClient, a.logger, cfg.Messaging.ChainPaymentTopic)
		subscriptionPublisher = dispatch.NewRedisStreamSubscriptionPublisher(redisClient, a.logger, cfg.Messaging.SubscriptionEventTopic)
		checkpoints = store.NewRedisCheckpointStore(redisClient, cfg.Redis.Prefix)
		activeDestinations = store.NewRedisActiveDestinationStore(redisClient, cfg.Redis.Prefix)
		if cfg.MySQLDSN != "" {
			if dbActiveDestinations, err := store.NewDBActiveDestinationStore(cfg.MySQLDSN); err == nil {
				activeDestinations = store.NewCompositeActiveDestinationStore(activeDestinations, dbActiveDestinations)
			} else {
				a.logger.Warn("db active destination fallback disabled", "error", err)
			}
		}
		instanceID, _ := os.Hostname()
		leader = coord.NewRedisLeaderElector(redisClient, cfg.Redis.Prefix, instanceID)
		locker = lock.NewRedisLocker(redisClient, instanceID)
	}

	instanceID, _ := os.Hostname()
	manager := watcher.NewManager(a.logger, publisher, subscriptionPublisher, checkpoints, activeDestinations, leader, locker, instanceID, cfg)
	manager.Register(&watcher.EVMWatcher{})
	manager.Register(&watcher.SolanaWatcher{})
	manager.Register(&watcher.TronWatcher{})
	manager.Register(&watcher.SuiWatcher{})
	manager.Register(&watcher.TonWatcher{})
	manager.Register(&watcher.SubscriptionWatcher{})

	if err := manager.Bootstrap(ctx); err != nil && !errors.Is(err, context.Canceled) {
		return err
	}

	a.logger.Info("watcher runtime started", "scanInterval", cfg.ScanInterval.String(), "chainCount", len(cfg.Chains))
	<-ctx.Done()
	a.logger.Info("watcher runtime stopped")
	return nil
}
