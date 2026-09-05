package model

import "time"

// ChainPaymentEvent is the normalized event contract between watcher and payment.
// Field names intentionally match the Java event boundary.
type ChainPaymentEvent struct {
	EventID            string    `json:"eventId"`
	Source             string    `json:"source"`
	Chain              string    `json:"chain"`
	Token              string    `json:"token"`
	TokenAddress       string    `json:"tokenAddress"`
	TxHash             string    `json:"txHash"`
	SourceAddress      string    `json:"sourceAddress"`
	DestinationAddress string    `json:"destinationAddress"`
	Amount             string    `json:"amount"`
	BlockNumber        int64     `json:"blockNumber"`
	LogIndex           int64     `json:"logIndex"`
	BlockTimestamp     time.Time `json:"blockTimestamp"`
	ObservedAt         time.Time `json:"observedAt"`
}
