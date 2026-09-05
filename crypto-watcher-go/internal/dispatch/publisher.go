package dispatch

import (
	"context"

	"cryptopay-gateway-watcher-go/internal/model"
)

// Publisher sends normalized chain events out of watcher.
type Publisher interface {
	Publish(ctx context.Context, event model.ChainPaymentEvent) error
}
