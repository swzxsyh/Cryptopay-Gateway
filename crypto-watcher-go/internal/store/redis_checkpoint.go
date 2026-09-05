package store

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

// RedisCheckpointStore persists watcher checkpoint data in redis.
type RedisCheckpointStore struct {
	client *redis.Client
	prefix string
}

func NewRedisCheckpointStore(client *redis.Client, prefix string) *RedisCheckpointStore {
	return &RedisCheckpointStore{client: client, prefix: prefix}
}

func (s *RedisCheckpointStore) Bootstrap(_ context.Context, chain string) error {
	return nil
}

func (s *RedisCheckpointStore) ObservedHeight(ctx context.Context, chain string) (int64, error) {
	return s.getInt(ctx, s.key(chain), "latestObservedBlock")
}

func (s *RedisCheckpointStore) ConfirmedHeight(ctx context.Context, chain string) (int64, error) {
	return s.getInt(ctx, s.key(chain), "lastConfirmedBlock")
}

func (s *RedisCheckpointStore) UpdateObserved(ctx context.Context, chain string, height int64) error {
	return s.updateMax(ctx, s.key(chain), "latestObservedBlock", height)
}

func (s *RedisCheckpointStore) UpdateConfirmed(ctx context.Context, chain string, height int64) error {
	return s.updateMax(ctx, s.key(chain), "lastConfirmedBlock", height)
}

func (s *RedisCheckpointStore) key(chain string) string {
	return fmt.Sprintf("%s:scanner:checkpoint:%s", s.prefix, strings.ToUpper(strings.TrimSpace(chain)))
}

func (s *RedisCheckpointStore) getInt(ctx context.Context, key string, field string) (int64, error) {
	value, err := s.client.HGet(ctx, key, field).Result()
	if err == redis.Nil {
		return 0, nil
	}
	if err != nil {
		return 0, err
	}
	n, convErr := strconv.ParseInt(value, 10, 64)
	if convErr != nil {
		return 0, convErr
	}
	return n, nil
}

func (s *RedisCheckpointStore) updateMax(ctx context.Context, key string, field string, height int64) error {
	script := redis.NewScript(`
local key = KEYS[1]
local field = ARGV[1]
local value = tonumber(ARGV[2])
local current = tonumber(redis.call("HGET", key, field) or "0")
if value > current then
  redis.call("HSET", key, field, tostring(value), "updatedAt", ARGV[3])
end
return 1
`)
	return script.Run(ctx, s.client, []string{key}, field, height, time.Now().UTC().Format(time.RFC3339Nano)).Err()
}
