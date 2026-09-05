package lock

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

// RedisLocker implements a simple redis lease lock.
type RedisLocker struct {
	client *redis.Client
	id     string
}

func NewRedisLocker(client *redis.Client, id string) *RedisLocker {
	return &RedisLocker{client: client, id: id}
}

func (r *RedisLocker) WithLock(ctx context.Context, key string, ttl time.Duration, fn func() error) error {
	ok, err := r.client.SetNX(ctx, key, r.id, ttl).Result()
	if err != nil {
		return err
	}
	if !ok {
		return fmt.Errorf("lock busy: %s", key)
	}
	defer r.release(ctx, key)
	if fn == nil {
		return errors.New("lock function is nil")
	}
	return fn()
}

func (r *RedisLocker) release(ctx context.Context, key string) {
	script := redis.NewScript(`
local key = KEYS[1]
local expected = ARGV[1]
if redis.call("GET", key) == expected then
  return redis.call("DEL", key)
end
return 0
`)
	_, _ = script.Run(ctx, r.client, []string{key}, r.id).Int()
}
