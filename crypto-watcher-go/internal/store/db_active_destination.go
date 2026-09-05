package store

import (
	"context"
	"database/sql"
	"strings"
	"time"

	_ "github.com/go-sql-driver/mysql"
)

// DBActiveDestinationStore is the same fallback semantic as Java ActiveDestinationAddressCache.
type DBActiveDestinationStore struct {
	db *sql.DB
}

func NewDBActiveDestinationStore(dsn string) (*DBActiveDestinationStore, error) {
	db, err := sql.Open("mysql", strings.TrimSpace(dsn))
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(4)
	db.SetMaxIdleConns(2)
	db.SetConnMaxLifetime(5 * time.Minute)
	return &DBActiveDestinationStore{db: db}, nil
}

func (s *DBActiveDestinationStore) Members(ctx context.Context, chain string, tokenAddress string) ([]string, error) {
	ctx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()
	rows, err := s.db.QueryContext(ctx, `
select payment_address
from payment_order
where chain = ?
  and token_address = ?
  and payment_address is not null
  and payment_address <> ''
  and status not in ('PAID','UNDERPAID','OVERPAID','CANCELLED')
union
select contract_address
from payment_order
where chain = ?
  and token_address = ?
  and contract_address is not null
  and contract_address <> ''
  and status not in ('PAID','UNDERPAID','OVERPAID','CANCELLED')`,
		strings.ToUpper(strings.TrimSpace(chain)),
		strings.TrimSpace(tokenAddress),
		strings.ToUpper(strings.TrimSpace(chain)),
		strings.TrimSpace(tokenAddress))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]string, 0)
	seen := make(map[string]struct{})
	for rows.Next() {
		var address string
		if err := rows.Scan(&address); err != nil {
			return nil, err
		}
		normalized := strings.ToLower(strings.TrimSpace(address))
		if normalized == "" {
			continue
		}
		if _, ok := seen[normalized]; ok {
			continue
		}
		seen[normalized] = struct{}{}
		out = append(out, normalized)
	}
	return out, rows.Err()
}
