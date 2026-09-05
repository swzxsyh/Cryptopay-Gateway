package store

import "context"

// CheckpointStore stores latest observed and confirmed heights.
type CheckpointStore interface {
	Bootstrap(ctx context.Context, chain string) error
	ObservedHeight(ctx context.Context, chain string) (int64, error)
	ConfirmedHeight(ctx context.Context, chain string) (int64, error)
	UpdateObserved(ctx context.Context, chain string, height int64) error
	UpdateConfirmed(ctx context.Context, chain string, height int64) error
}
