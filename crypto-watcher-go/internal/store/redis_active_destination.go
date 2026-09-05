package store

import (
	"context"
	"fmt"
	"strings"

	"github.com/redis/go-redis/v9"
)

// RedisActiveDestinationStore uses the same set key format as the Java payment core.
type RedisActiveDestinationStore struct {
	client *redis.Client
	prefix string
}

func NewRedisActiveDestinationStore(client *redis.Client, prefix string) *RedisActiveDestinationStore {
	return &RedisActiveDestinationStore{client: client, prefix: prefix}
}

func (s *RedisActiveDestinationStore) Members(ctx context.Context, chain string, tokenAddress string) ([]string, error) {
	return s.client.SMembers(ctx, s.key(chain, tokenAddress)).Result()
}

func (s *RedisActiveDestinationStore) AddMany(ctx context.Context, chain string, tokenAddress string, addresses []string) error {
	if len(addresses) == 0 {
		return nil
	}
	values := make([]any, 0, len(addresses))
	for _, address := range addresses {
		normalized := strings.ToLower(strings.TrimSpace(address))
		if normalized != "" {
			values = append(values, normalized)
		}
	}
	if len(values) == 0 {
		return nil
	}
	return s.client.SAdd(ctx, s.key(chain, tokenAddress), values...).Err()
}

func (s *RedisActiveDestinationStore) key(chain string, tokenAddress string) string {
	address := strings.ToLower(strings.TrimSpace(tokenAddress))
	if address == "" {
		address = "native"
	}
	return fmt.Sprintf("%s:active-destination:%s:%s", s.prefix, strings.ToUpper(strings.TrimSpace(chain)), address)
}
