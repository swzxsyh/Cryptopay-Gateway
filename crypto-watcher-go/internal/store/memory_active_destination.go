package store

import "context"

// MemoryActiveDestinationStore is a no-op fallback for local dry-run mode.
type MemoryActiveDestinationStore struct{}

func NewMemoryActiveDestinationStore() *MemoryActiveDestinationStore {
	return &MemoryActiveDestinationStore{}
}

func (s *MemoryActiveDestinationStore) Members(_ context.Context, _ string, _ string) ([]string, error) {
	return nil, nil
}
