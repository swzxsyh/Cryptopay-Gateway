package dispatch

import (
	"context"
	"log/slog"
	"sync"

	"cryptopay-gateway-watcher-go/internal/model"
)

// MemoryPublisher is a temporary publisher used for local development.
type MemoryPublisher struct {
	logger *slog.Logger
	mu     sync.Mutex
	events []model.ChainPaymentEvent
}

func NewMemoryPublisher(logger *slog.Logger) *MemoryPublisher {
	if logger == nil {
		logger = slog.Default()
	}
	return &MemoryPublisher{logger: logger}
}

func (p *MemoryPublisher) Publish(_ context.Context, event model.ChainPaymentEvent) error {
	p.mu.Lock()
	p.events = append(p.events, event)
	p.mu.Unlock()
	p.logger.Info("published chain event", "eventID", event.EventID, "chain", event.Chain, "txHash", event.TxHash, "source", event.Source)
	return nil
}
