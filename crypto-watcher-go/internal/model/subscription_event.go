package model

import "time"

// SubscriptionEvent is the normalized event contract for on-chain subscription lifecycle events.
type SubscriptionEvent struct {
	EventID        string    `json:"eventId"`
	Source         string    `json:"source"`
	Chain          string    `json:"chain"`
	Contract       string    `json:"contract"`
	EventType      string    `json:"eventType"`
	SubscriptionID string    `json:"subscriptionId"`
	TxHash         string    `json:"txHash"`
	LogIndex       int64     `json:"logIndex"`
	BlockNumber    int64     `json:"blockNumber"`
	BlockTimestamp time.Time `json:"blockTimestamp"`
	Sequence       string    `json:"sequence"`
	AmountRaw      string    `json:"amountRaw"`
	ObservedAt     time.Time `json:"observedAt"`
}
