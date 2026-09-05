package dispatch

import (
	"context"
	"encoding/json"
	"log/slog"
	"time"

	"cryptopay-gateway-watcher-go/internal/model"
	"github.com/redis/go-redis/v9"
)

// SubscriptionPublisher publishes normalized subscription lifecycle events.
type SubscriptionPublisher interface {
	PublishSubscription(ctx context.Context, event model.SubscriptionEvent) error
}

type NoopSubscriptionPublisher struct{}

func (p NoopSubscriptionPublisher) PublishSubscription(_ context.Context, _ model.SubscriptionEvent) error {
	return nil
}

// RedisStreamSubscriptionPublisher publishes subscription events to a dedicated Redis Stream.
type RedisStreamSubscriptionPublisher struct {
	client *redis.Client
	logger *slog.Logger
	stream string
}

func NewRedisStreamSubscriptionPublisher(client *redis.Client, logger *slog.Logger, stream string) *RedisStreamSubscriptionPublisher {
	if logger == nil {
		logger = slog.Default()
	}
	return &RedisStreamSubscriptionPublisher{client: client, logger: logger, stream: stream}
}

func (p *RedisStreamSubscriptionPublisher) PublishSubscription(ctx context.Context, event model.SubscriptionEvent) error {
	payload, err := json.Marshal(event)
	if err != nil {
		return err
	}
	_, err = p.client.XAdd(ctx, &redis.XAddArgs{
		Stream: p.stream,
		Values: map[string]any{
			"eventId":        event.EventID,
			"chain":          event.Chain,
			"txHash":         event.TxHash,
			"eventType":      event.EventType,
			"subscriptionId": event.SubscriptionID,
			"payload":        string(payload),
			"ts":             time.Now().UnixMilli(),
		},
	}).Result()
	if err != nil {
		return err
	}
	p.logger.Info("published subscription event to redis stream", "stream", p.stream, "eventID", event.EventID, "chain", event.Chain, "txHash", event.TxHash, "eventType", event.EventType)
	return nil
}
