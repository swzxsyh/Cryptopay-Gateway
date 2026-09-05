package store

import "context"

// ActiveDestinationStore reads active receiving addresses that still need chain matching.
type ActiveDestinationStore interface {
	Members(ctx context.Context, chain string, tokenAddress string) ([]string, error)
}
