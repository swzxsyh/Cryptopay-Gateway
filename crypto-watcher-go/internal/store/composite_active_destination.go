package store

import "context"

type activeDestinationCacheWriter interface {
	AddMany(ctx context.Context, chain string, tokenAddress string, addresses []string) error
}

// CompositeActiveDestinationStore reads Redis first and falls back to DB when Redis is cold.
type CompositeActiveDestinationStore struct {
	primary  ActiveDestinationStore
	fallback ActiveDestinationStore
}

func NewCompositeActiveDestinationStore(primary ActiveDestinationStore, fallback ActiveDestinationStore) *CompositeActiveDestinationStore {
	return &CompositeActiveDestinationStore{primary: primary, fallback: fallback}
}

func (s *CompositeActiveDestinationStore) Members(ctx context.Context, chain string, tokenAddress string) ([]string, error) {
	if s.primary != nil {
		members, err := s.primary.Members(ctx, chain, tokenAddress)
		if err != nil {
			return nil, err
		}
		if len(members) > 0 {
			return members, nil
		}
	}
	if s.fallback == nil {
		return nil, nil
	}
	members, err := s.fallback.Members(ctx, chain, tokenAddress)
	if err != nil {
		return nil, err
	}
	if len(members) > 0 {
		if writer, ok := s.primary.(activeDestinationCacheWriter); ok {
			_ = writer.AddMany(ctx, chain, tokenAddress, members)
		}
	}
	return members, nil
}
