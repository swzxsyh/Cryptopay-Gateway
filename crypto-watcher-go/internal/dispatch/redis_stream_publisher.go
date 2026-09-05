package dispatch

import (
	"context"
	"encoding/json"
	"log/slog"
	"time"

	"cryptopay-gateway-watcher-go/internal/model"
	"github.com/redis/go-redis/v9"
)

// RedisStreamPublisher publishes events into a Redis Stream for payment consumption.
type RedisStreamPublisher struct {
	client *redis.Client
	logger *slog.Logger
	stream string
}

func NewRedisStreamPublisher(client *redis.Client, logger *slog.Logger, stream string) *RedisStreamPublisher {
	if logger == nil {
		logger = slog.Default()
	}
	return &RedisStreamPublisher{client: client, logger: logger, stream: stream}
}

func (p *RedisStreamPublisher) Publish(ctx context.Context, event model.ChainPaymentEvent) error {
	payload, err := json.Marshal(event)
	if err != nil {
		return err
	}
	_, err = p.client.XAdd(ctx, &redis.XAddArgs{
		Stream: p.stream,
		Values: map[string]any{
			"eventId": event.EventID,
			"source":  event.Source,
			"chain":   event.Chain,
			"txHash":  event.TxHash,
			"payload": string(payload),
			"ts":      time.Now().UnixMilli(),
		},
	}).Result()
	if err != nil {
		return err
	}
	p.logger.Info("published chain event to redis stream", "stream", p.stream, "eventID", event.EventID, "chain", event.Chain, "txHash", event.TxHash)
	return nil
}
