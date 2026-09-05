# crypto-watcher-go

独立的 Go 版 watcher 工程。当前保留 Java watcher，不做强制替换；Go watcher 通过同一个 Redis Stream 向 `crypto-payment` 投递统一链上入账消息。

## 当前已实现

- Watcher Manager：按链族加载不同 watcher。
- EVM watcher：基于 `go-ethereum` 使用 `eth_getLogs` 扫描 ERC20 `Transfer` 日志。
- EVM WSS newHeads：可选开启，用于推进最新观察块。
- Solana watcher：基于 `gagliardetto/solana-go` 按收款地址查询 finalized signature，并通过 SPL Token 余额变化识别入账。
- TRON watcher：按 TronGrid 兼容事件 API 读取 TRC20 `Transfer` 事件，按活跃收款地址过滤后输出统一入账消息。
- SUI watcher：按 `suix_queryEvents` 读取配置的 CoinType/MoveEventType 事件，适合把 `payment_token_config.token_address` 配为 SUI CoinType 或项目合约事件类型。
- TON watcher：兼容 TonCenter `getTransactions` 与 Jetton Indexer `transfers/events` 形态，按活跃地址过滤后输出统一入账消息。
- Subscription watcher：基于 `go-ethereum` 精确解析订阅合约事件，识别激活、续扣、暂停、恢复、取消。
- Redis Stream Publisher：消息写入 `crypto:payment:chain-payment-events`，由 Java `crypto-payment` 消费。
- Redis checkpoint：兼容 Java hash key `crypto:payment:scanner:checkpoint:{CHAIN}`。
- Redis leader election：兼容 Java WSS leader key `crypto:payment:watcher:wss:leader:{CHAIN}`。
- Redis distributed lock：EVM/Solana 扫描使用链级锁，避免多实例重复扫描同一批块。
- Active destination：兼容 Java Redis Set `crypto:payment:active-destination:{CHAIN}:{tokenAddress}`，只扫描仍需匹配的收款地址。
- 单链故障隔离：每条链独立 goroutine 运行，EVM 连接失败或 WSS 订阅断开会按链冷却重试，不阻塞其它链。
- DB 配置加载：支持从 `payment_chain_config`、`payment_token_config`、`payment_scanner_config`、`payment_chain_scanner_config` 加载链、Token、扫描参数。
- DB 活跃地址兜底：Redis 活跃地址 Set 为空时，可从 `payment_order` 回查未结束订单的收款地址并回填 Redis。

## 统一消息字段

Go watcher 输出的 `payload` 与 Java `ChainPaymentEvent` 保持字段一致：

- `eventId`
- `source`
- `chain`
- `token`
- `tokenAddress`
- `txHash`
- `sourceAddress`
- `destinationAddress`
- `amount`
- `blockNumber`
- `logIndex`
- `blockTimestamp`
- `observedAt`

不要随意向 payload 增加字段。payment 端状态机、防重放、KYT、记账、回调都依赖这一层边界稳定。

## 配置示例

PowerShell:

```powershell
$env:WATCHER_REDIS_ADDR="127.0.0.1:6379"
$env:WATCHER_REDIS_PREFIX="crypto:payment"
$env:WATCHER_CHAIN_PAYMENT_TOPIC="crypto:payment:chain-payment-events"
$env:WATCHER_SUBSCRIPTION_EVENT_TOPIC="crypto:payment:subscription-events"
$env:WATCHER_SCAN_INTERVAL_SECONDS="15"
$env:WATCHER_BACKFILL_BLOCKS="12"
$env:WATCHER_LOG_BATCH_BLOCKS="50"
$env:WATCHER_LOG_RETRY_ATTEMPTS="3"
$env:WATCHER_USE_WEBSOCKET_HEADS="true"

$env:WATCHER_CHAINS="ETH_SEPOLIA,SOLANA_DEVNET"
$env:WATCHER_CHAIN_ETH_SEPOLIA_FAMILY="EVM"
$env:WATCHER_CHAIN_ETH_SEPOLIA_RPC_URL="https://eth-sepolia.g.alchemy.com/v2/xxx"
$env:WATCHER_CHAIN_ETH_SEPOLIA_WS_URL="wss://eth-sepolia.g.alchemy.com/v2/xxx"
$env:WATCHER_CHAIN_ETH_SEPOLIA_CONFIRMATION_DEPTH="12"
$env:WATCHER_CHAIN_SOLANA_DEVNET_FAMILY="SOLANA"
$env:WATCHER_CHAIN_SOLANA_DEVNET_RPC_URL="https://api.devnet.solana.com"
$env:WATCHER_CHAIN_SOLANA_DEVNET_CONFIRMATION_DEPTH="32"
$env:WATCHER_SUBSCRIPTION_ENABLED="true"
$env:WATCHER_SUBSCRIPTION_CONTRACTS="0xYourErc1337Executor,0xYourSuperfluidAdapter"

$env:WATCHER_RULES_JSON='[
  {"chain":"ETH_SEPOLIA","family":"EVM","token":"USDC","tokenAddress":"0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238","decimals":6},
  {"chain":"SOLANA_DEVNET","family":"SOLANA","token":"USDC","tokenAddress":"So11111111111111111111111111111111111111112","decimals":6}
]'
```

如果 `WATCHER_RULES_JSON` 里没有 `destinations`，watcher 会读取 payment 维护的活跃收款地址 Redis Set。

DB 配置模式：

```powershell
$env:WATCHER_CONFIG_SOURCE="DB"
$env:WATCHER_MYSQL_DSN="user:password@tcp(127.0.0.1:3306)/crypto?parseTime=true&loc=Local"
$env:WATCHER_REDIS_ADDR="127.0.0.1:6379"
```

DB 模式下：

- 链 RPC/WSS 来自 `payment_chain_config`。
- 全局扫描参数来自 `payment_scanner_config`。
- 单链覆盖参数来自 `payment_chain_scanner_config`，不会污染其它链。
- Token/Mint 监听规则来自 `payment_token_config`，Token 级确认数会参与链确认深度计算。
- 活跃收款地址仍来自 Redis Set，由 payment 创建订单和地址池流程维护。
- 订阅事件合约来自 `payment_subscription_config` 的 `erc1337_executor_address`、`superfluid_cfa_address`、`superfluid_host_address`，也可用 `WATCHER_SUBSCRIPTION_CONTRACTS` 覆盖。

TRON / TON / SUI 配置注意：

- TRON：`payment_chain_config.rpc_url` 建议配置为 TronGrid 兼容 API 根地址，例如 `https://api.trongrid.io`；如需 API Key，设置 `WATCHER_TRON_API_KEY`。
- SUI：`payment_chain_config.rpc_url` 配 SUI JSON-RPC；`payment_token_config.token_address` 配 CoinType/MoveEventType。
- TON：`payment_chain_config.rpc_url` 配 TonCenter API 根地址，或 Jetton Indexer 查询地址；如需 API Key，设置 `WATCHER_TON_API_KEY`。
- 非 EVM 链的 `token_address` 不是 EVM 合约地址，含义由链族决定：SUI 是 CoinType/MoveEventType，TON 是 Jetton Master/Indexer token 标识，TRON 是 TRC20 合约地址。

## 启动

```powershell
$env:GOROOT="D:\sdk\go1.25.3"
$env:Path="D:\sdk\go1.25.3\bin;$env:Path"
go run ./cmd/watcher
```

## 状态机边界

Go watcher 只负责发现链上事件并发送统一消息，不直接改订单状态、不做入账、不触发回调。订单状态机仍由 `crypto-payment` 消费端统一处理：

- 防重放：按 `chain + txHash` / `chain + txHash + logIndex` 做唯一语义。
- 提前发现与确认数：由 payment pending confirmation 模块处理。
- KYT 前置/后置、异常单、地址冻结、余额记账、商户回调：仍在 payment/core 中闭环。
- 订阅事件：Go watcher 发布到 `crypto:payment:subscription-events`，payment 侧 `RedisStreamSubscriptionEventSubscriber` 消费后调用订阅状态机和账单确认服务。

## 与 Java watcher 的差异

- Go EVM 已对齐确认后 ERC20 入账事件，但暂未实现原生币全块交易扫描。
- Go Solana 已对齐确认后 SPL Token 入账事件，但暂未实现原生 SOL 入账扫描。
- Java watcher 会扫描未确认区块并写入 pending confirmation；Go watcher 当前只投递已满足确认数的最终事件。
- Java watcher 已接订阅事件扫描服务；Go subscription watcher 已支持按合约 ABI topic 精确解析。
- TRON / TON / SUI 已有独立 watcher，但生产效果取决于对应 RPC/Indexer 是否提供标准事件字段。

## 待补充

- TRON gRPC：当前先走 TronGrid 兼容 HTTP Event API；如部署自建 fullnode/event-server，可新增 gRPC client 替换 `TronWatcher` 内部 event client。
- TON Jetton 深度解析：当前兼容 TonCenter/Indexer 常见响应，生产建议接专用 indexer 获取标准 Jetton transfer 字段。
- SUI 精确事件 ABI：当前按 CoinType/MoveEventType 和常见 `sender/from/to/recipient/amount` 字段解析；若你的 SUI 合约事件字段名不同，需要新增 parser profile。
- 订阅事件消费幂等表：当前依赖 Redis Stream pending/ACK 和订阅订单/账单状态机，后续可增加 `subscription_chain_event_record` 做事件级审计与重放控制。
- 有界投递队列：当前发布是同步写 Redis Stream，可靠性优先；如遇块风暴，可再增加带 ACK 语义的本地有界队列或 worker pool。
- 生产级指标：补 Prometheus metrics、链级延迟、扫描积压、RPC 错误率、Stream pending 监控。
