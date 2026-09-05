package store

import (
	"context"
	"sync"
)

// MemoryCheckpointStore is a local development checkpoint store.
type MemoryCheckpointStore struct {
	mu        sync.RWMutex
	observed  map[string]int64
	confirmed map[string]int64
}

func NewMemoryCheckpointStore() *MemoryCheckpointStore {
	return &MemoryCheckpointStore{
		observed:  make(map[string]int64),
		confirmed: make(map[string]int64),
	}
}

func (s *MemoryCheckpointStore) Bootstrap(_ context.Context, chain string) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.observed[chain]; !ok {
		s.observed[chain] = 0
	}
	if _, ok := s.confirmed[chain]; !ok {
		s.confirmed[chain] = 0
	}
	return nil
}

func (s *MemoryCheckpointStore) ObservedHeight(_ context.Context, chain string) (int64, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.observed[chain], nil
}

func (s *MemoryCheckpointStore) ConfirmedHeight(_ context.Context, chain string) (int64, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	return s.confirmed[chain], nil
}

func (s *MemoryCheckpointStore) UpdateObserved(_ context.Context, chain string, height int64) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if height > s.observed[chain] {
		s.observed[chain] = height
	}
	return nil
}

func (s *MemoryCheckpointStore) UpdateConfirmed(_ context.Context, chain string, height int64) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	if height > s.confirmed[chain] {
		s.confirmed[chain] = height
	}
	return nil
}
