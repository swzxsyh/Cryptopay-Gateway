# CryptoPay Gateway

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[English](README.md)

CryptoPay Gateway 是一个模块化的加密货币支付后端，用于接收和结算 Token
支付。项目提供商户订单接口、收银台流程、链上支付监听、幂等支付处理、回调通知及运营管理接口。

主要收款流程面向 USDC 和 USDT。Java 负责业务逻辑和支付状态机；独立的 Go
watcher 可作为生产环境的链上扫描服务部署。

> 本项目是软件基础设施，并非托管服务或金融产品。上线前请审查代码、配置、支持的链和
> Token 合约，并遵守适用的法律法规。

## 功能特性

- 支持商户签名的支付订单创建与查询接口
- 使用收银台 Token 保护支付方式选择及钱包操作
- 支持 EVM、Solana、TRON、SUI、TON 链族的 USDC/USDT Token 路由
- 支持派生地址和合约结算两种支付方式
- 使用 Redis Stream 解耦链上 watcher 与支付处理
- 支付状态流转、幂等控制、金额校验、KYT 钩子、账本记账和签名商户回调
- 可配置确认数、扫描 checkpoint、活跃收款地址、分布式锁和交易防重放
- Go watcher 支持 EVM ERC20、Solana SPL、TRC20、SUI、TON 和订阅事件
- 管理端提供 JWT 认证和权限控制

## 架构

```text
商户接口
    |
    v
crypto-payment  --->  MySQL（订单、账本、审计记录）
    |                         ^
    |                         |
    +--> 收银台接口            |
    |                         |
    v                         |
Redis：活跃地址、checkpoint、锁、Redis Streams
    ^
    |
crypto-watcher-go（推荐的生产扫描器）
    |
    v
区块链 RPC / WebSocket 服务商
```

watcher 向 `crypto:payment:chain-payment-events` 发布统一的链上支付事实。
`crypto-payment` 是订单匹配、状态变更、记账、KYT 决策和回调通知的唯一处理方。

## 目录结构

| 目录 | 说明 |
| --- | --- |
| `crypto-core` | 支付领域模型、持久化、配置、安全能力和链集成 |
| `crypto-payment` | 商户、收银台、网关、结算、回调和链上支付事件服务 |
| `crypto-watcher` | Java 链上监听实现 |
| `crypto-watcher-go` | 独立的 Go 链上监听运行时 |
| `crypto-manager` | 管理端 API、JWT 和权限控制 |
| `crypto-secret-jni` | Rust JNI 原生密钥处理库 |
| `docs` | MySQL Schema 和本地初始化 SQL |

## 环境要求

- JDK 25
- Maven 3.6.3+
- Go 1.25.3+（运行 `crypto-watcher-go`）
- Rust toolchain（构建 `crypto-secret-jni`）
- MySQL 8+
- Redis 7+
- 已启用链对应的 RPC Endpoint

## 快速开始

1. 使用 `docs/mysql-schema.sql` 初始化开发数据库；可选初始化数据见
   `docs/mysql-init-clean.sql`。
2. 基于相关模块的 `application-example.yml` 补充配置。凭据、私钥和服务商 Token
   必须保存在版本控制之外。
3. 构建 Java 模块：

   ```bash
   mvn clean install
   ```

4. 启动支付服务：

   ```bash
   mvn spring-boot:run -pl crypto-payment
   ```

5. 配置并启动**一种** watcher 实现。启动 Go watcher：

   ```bash
   cd crypto-watcher-go
   go run ./cmd/watcher
   ```

Go watcher 的检查命令：

```bash
cd crypto-watcher-go
go test ./...
go build ./...
```

## Watcher 配置

Go watcher 通过环境变量读取配置。至少需要配置 Redis、启用的链和 Token 合约：

```bash
export WATCHER_REDIS_ADDR="127.0.0.1:6379"
export WATCHER_REDIS_PREFIX="crypto:payment"
export WATCHER_CHAIN_PAYMENT_TOPIC="crypto:payment:chain-payment-events"
export WATCHER_CONFIG_SOURCE="DB"
export WATCHER_MYSQL_DSN="user:password@tcp(127.0.0.1:3306)/crypto?parseTime=true&loc=Local"
```

在 `DB` 模式下，watcher 从 `payment_chain_config`、`payment_token_config`、
`payment_scanner_config` 和 `payment_chain_scanner_config` 加载配置。活跃收款地址由
`crypto-payment` 写入 Redis；Redis 缓存为空时，watcher 会从 MySQL 中的未完成订单回查。

完整的环境变量和链专属配置请参阅
[`crypto-watcher-go/README.md`](crypto-watcher-go/README.md)。

## 安全

生产环境应遵守以下要求：

- 启用商户请求签名校验，并妥善保护签名私钥。
- 对公开 API、回调、Redis、数据库和 RPC 连接使用 TLS。
- 将数据库、Redis 和管理端接口置于私有网络中。
- 为每个商户配置 HTTPS 回调域名白名单。拒绝解析到私网、回环、链路本地或保留网段的
  回调地址。
- 未配置高强度 secret 且未进行独立链上交易校验时，不要启用 Helius webhook。
- 在 Token 转账的来源账户及授权方已与订单钱包完成校验前，不要启用 Solana Fee Payer
  签名。
- 每条链只能运行 Java watcher 或 Go watcher 其中之一，不能同时运行。
- 尽可能为每笔活跃支付订单使用独立的收款地址。

如需私下报告安全漏洞，请在 GitHub 创建 Security Advisory，不要公开提交 Issue。

## 开发说明

- 不要提交 `application-dev.yml`、本地环境文件、私钥、证书、keystore 或日志。
- Go watcher 是轻量化扫描运行时，但仍需根据目标交易量为 RPC 服务商限额和 Redis Stream
  保留策略进行容量规划和监控。
- 启用批量付款场景前，需要先完成单笔交易内多个 Token Transfer 的事件级幂等设计。

## 开源许可证

CryptoPay Gateway 使用 [MIT License](LICENSE) 开源。
