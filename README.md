# CryptoPay Gateway

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[中文文档](README.zh-CN.md)

CryptoPay Gateway is a modular cryptocurrency payment backend for accepting and
settling token payments. It provides merchant order APIs, cashier flows,
on-chain payment detection, idempotent payment processing, callbacks, and an
operations API.

The primary receiving flow is designed for USDC and USDT token payments. Java
implements the business and payment state machine; the standalone Go watcher
can be deployed as the production on-chain scanner.

> This project is software infrastructure, not a custodial service or financial
> product. Review the code, configuration, supported chain/token contracts, and
> applicable regulations before any production deployment.

## Features

- Merchant-signed payment order creation and query APIs
- Cashier-token-protected payment route selection and wallet actions
- USDC/USDT token routing for EVM, Solana, TRON, SUI, and TON chain families
- Derived-address and contract settlement payment methods
- Redis Stream event boundary between chain watchers and payment processing
- Payment state transitions, idempotency, amount checks, KYT hooks, ledger
  posting, and signed merchant callbacks
- Configurable confirmation depth, checkpoints, active destination addresses,
  distributed locks, and replay protection
- Go watcher runtime for EVM ERC20, Solana SPL, TRC20, SUI, TON, and
  subscription events
- Administrative API with JWT authentication and permission controls

## Architecture

```text
Merchant API
    |
    v
crypto-payment  --->  MySQL (orders, ledger, audit records)
    |                         ^
    |                         |
    +--> Cashier API           |
    |                         |
    v                         |
Redis: active destinations, checkpoints, locks, Redis Streams
    ^
    |
crypto-watcher-go (recommended production scanner)
    |
    v
Blockchain RPC / WebSocket providers
```

The watcher publishes normalized payment facts to
`crypto:payment:chain-payment-events`. `crypto-payment` is the sole owner of
order matching, payment status changes, accounting, KYT decisions, and callback
delivery.

## Repository Layout

| Path | Description |
| --- | --- |
| `crypto-core` | Shared payment domain, persistence, configuration, security, and chain integrations |
| `crypto-payment` | Merchant, cashier, gateway, settlement, callback, and payment event services |
| `crypto-watcher` | Java chain watcher implementation |
| `crypto-watcher-go` | Standalone Go chain watcher runtime |
| `crypto-manager` | Administrative APIs and JWT-based management security |
| `crypto-secret-jni` | Rust JNI library for native secret handling |
| `docs` | MySQL schema and local initialization SQL |

## Requirements

- JDK 25
- Maven 3.6.3+
- Go 1.25.3+ for `crypto-watcher-go`
- Rust toolchain for `crypto-secret-jni`
- MySQL 8+
- Redis 7+
- RPC endpoints for each enabled chain

## Quick Start

1. Initialize a development database with `docs/mysql-schema.sql`. Optional
   seed data is available in `docs/mysql-init-clean.sql`.
2. Copy and complete the module configuration from the relevant
   `application-example.yml`. Keep credentials, private keys, and provider
   tokens outside version control.
3. Build the Java modules:

   ```bash
   mvn clean install
   ```

4. Start the payment service:

   ```bash
   mvn spring-boot:run -pl crypto-payment
   ```

5. Configure and start **one** watcher implementation. For the Go runtime:

   ```bash
   cd crypto-watcher-go
   go run ./cmd/watcher
   ```

Run the Go watcher checks with:

```bash
cd crypto-watcher-go
go test ./...
go build ./...
```

## Watcher Configuration

The Go watcher reads environment variables. At minimum, configure Redis, the
enabled chains, and the token contracts:

```bash
export WATCHER_REDIS_ADDR="127.0.0.1:6379"
export WATCHER_REDIS_PREFIX="crypto:payment"
export WATCHER_CHAIN_PAYMENT_TOPIC="crypto:payment:chain-payment-events"
export WATCHER_CONFIG_SOURCE="DB"
export WATCHER_MYSQL_DSN="user:password@tcp(127.0.0.1:3306)/crypto?parseTime=true&loc=Local"
```

In `DB` mode, watcher configuration is read from `payment_chain_config`,
`payment_token_config`, `payment_scanner_config`, and
`payment_chain_scanner_config`. Active receiving addresses are supplied by
`crypto-payment` through Redis and fall back to active orders in MySQL.

See [`crypto-watcher-go/README.md`](crypto-watcher-go/README.md) for the full
environment-variable reference and chain-specific configuration.

## Security

Production deployments should follow these requirements:

- Enable merchant request signature verification and protect signing keys.
- Use TLS for all public APIs, callbacks, Redis, database, and RPC connections.
- Restrict database, Redis, and management endpoints to private networks.
- Configure an HTTPS callback-domain allowlist for every merchant. Do not allow
  callback URLs that resolve to private, loopback, link-local, or reserved
  network ranges.
- Do not enable the Helius webhook without a strong secret and independent
  on-chain transaction validation.
- Do not enable Solana Fee Payer signing for token transfers until the transfer
  source account and authority are verified against the order wallet.
- Run either the Java watcher or the Go watcher for a chain, never both.
- Use a unique receiving address per active payment order where possible.

To report a vulnerability privately, open a GitHub Security Advisory rather
than filing a public issue.

## Development Notes

- `application-dev.yml`, local environment files, private keys, certificates,
  keystores, and logs must not be committed.
- The Go watcher is optimized as a lightweight scanner runtime, but RPC provider
  limits and Redis Stream retention must be sized and monitored for the target
  transaction volume.
- A single transaction containing multiple token transfers needs event-level
  idempotency consideration before enabling batch-payout use cases.

## License

CryptoPay Gateway is released under the [MIT License](LICENSE).
