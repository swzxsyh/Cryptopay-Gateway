package coord

import (
	"context"
	"strings"
	"time"

	"github.com/redis/go-redis/v9"
)

// RedisLeaderElector implements a simple lease-based leader election.
type RedisLeaderElector struct {
	client *redis.Client
	prefix string
	id     string
}

func NewRedisLeaderElector(client *redis.Client, prefix, id string) *RedisLeaderElector {
	return &RedisLeaderElector{client: client, prefix: prefix, id: id}
}

func (r *RedisLeaderElector) TryAcquire(ctx context.Context, chain string, ttl time.Duration) (bool, error) {
	return r.client.SetNX(ctx, r.key(chain), r.id, ttl).Result()
}

func (r *RedisLeaderElector) Renew(ctx context.Context, chain string, ttl time.Duration) (bool, error) {
	script := redis.NewScript(`
local key = KEYS[1]
local expected = ARGV[1]
local ttl = tonumber(ARGV[2])
if redis.call("GET", key) == expected then
  redis.call("PEXPIRE", key, ttl)
  return 1
end
return 0
`)
	out, err := script.Run(ctx, r.client, []string{r.key(chain)}, r.id, ttl.Milliseconds()).Int()
	return out == 1, err
}

func (r *RedisLeaderElector) Release(ctx context.Context, chain string) error {
	script := redis.NewScript(`
local key = KEYS[1]
local expected = ARGV[1]
if redis.call("GET", key) == expected then
  return redis.call("DEL", key)
end
return 0
`)
	_, err := script.Run(ctx, r.client, []string{r.key(chain)}, r.id).Int()
	return err
}

func (r *RedisLeaderElector) Current(ctx context.Context, chain string) (string, error) {
	return r.client.Get(ctx, r.key(chain)).Result()
}

func (r *RedisLeaderElector) key(chain string) string {
	return r.prefix + ":watcher:wss:leader:" + strings.ToUpper(strings.TrimSpace(chain))
}
