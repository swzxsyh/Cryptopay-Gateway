package watcher

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"strings"

	"cryptopay-gateway-watcher-go/internal/config"
	"cryptopay-gateway-watcher-go/internal/coord"
	"cryptopay-gateway-watcher-go/internal/dispatch"
	"cryptopay-gateway-watcher-go/internal/lock"
	"cryptopay-gateway-watcher-go/internal/store"
)

// Manager orchestrates all watcher strategies.
type Manager struct {
	logger       *slog.Logger
	publisher    dispatch.Publisher
	subPublisher dispatch.SubscriptionPublisher
	checkpoint   store.CheckpointStore
	activeDest   store.ActiveDestinationStore
	leader       coord.LeaderElector
	locker       lock.Locker
	cfg          config.Config
	nodeID       string
	watchers     []ChainWatcher
}

func NewManager(
	logger *slog.Logger,
	publisher dispatch.Publisher,
	subPublisher dispatch.SubscriptionPublisher,
	checkpoint store.CheckpointStore,
	activeDest store.ActiveDestinationStore,
	leader coord.LeaderElector,
	locker lock.Locker,
	nodeID string,
	cfg config.Config,
) *Manager {
	if logger == nil {
		logger = slog.Default()
	}
	return &Manager{
		logger:       logger,
		publisher:    publisher,
		subPublisher: subPublisher,
		checkpoint:   checkpoint,
		activeDest:   activeDest,
		leader:       leader,
		locker:       locker,
		nodeID:       nodeID,
		cfg:          cfg,
	}
}

func (m *Manager) Register(watcher ChainWatcher) {
	if watcher == nil {
		return
	}
	m.watchers = append(m.watchers, watcher)
}

func (m *Manager) Bootstrap(ctx context.Context) error {
	if len(m.cfg.Chains) == 0 {
		return nil
	}
	for _, profile := range m.cfg.Chains {
		if !profile.Enabled {
			continue
		}
		if err := m.checkpoint.Bootstrap(ctx, profile.Chain); err != nil {
			return fmt.Errorf("bootstrap checkpoint for %s: %w", profile.Chain, err)
		}
		for _, watcher := range m.matching(profile) {
			if err := watcher.Bootstrap(ctx, m.dependencies(), profile); err != nil {
				return fmt.Errorf("bootstrap watcher %s for %s: %w", watcher.Name(), profile.Chain, err)
			}
		}
	}
	return nil
}

func (m *Manager) Scan(ctx context.Context, profile config.ChainProfile) {
	if !profile.Enabled || strings.TrimSpace(profile.Chain) == "" {
		return
	}
	for _, watcher := range m.matching(profile) {
		if err := watcher.Scan(ctx, m.dependencies(), profile); err != nil && !errors.Is(err, context.Canceled) {
			m.logger.Warn("watcher scan failed", "watcher", watcher.Name(), "chain", profile.Chain, "error", err)
		}
	}
}

func (m *Manager) dependencies() Dependencies {
	return Dependencies{
		Logger:       m.logger,
		Publisher:    m.publisher,
		SubPublisher: m.subPublisher,
		Checkpoint:   m.checkpoint,
		ActiveDest:   m.activeDest,
		Leader:       m.leader,
		Locker:       m.locker,
		Config:       m.cfg,
		NodeID:       m.nodeID,
	}
}

func (m *Manager) matching(profile config.ChainProfile) []ChainWatcher {
	out := make([]ChainWatcher, 0, len(m.watchers))
	for _, watcher := range m.watchers {
		if watcher.Supports(profile) {
			out = append(out, watcher)
		}
	}
	return out
}
