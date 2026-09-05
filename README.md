# CryptoPay Gateway

CryptoPay Gateway 是一个面向加密货币支付与链上监控的多模块后端项目，包含 Java 业务模块、Go 监控服务以及 Rust JNI 安全组件。

## 项目概览

该项目聚焦于以下能力：
- 加密货币支付订单创建与状态跟踪
- 链上交易监控与事件消费
- 钱包/地址管理与链路路由
- 商户与结算相关能力
- 安全密钥/敏感信息的原生 JNI 处理

## 技术栈

- Java / Spring Boot 4
- Maven 多模块构建
- MySQL / MyBatis Plus
- Redis / Redisson
- Web3 / Solana / 链上扫描能力
- Go watcher runtime
- Rust JNI native secret library

## 代码结构

```text
CryptoPay-Gateway/
├── pom.xml                         # Maven parent
├── .gitignore                     # Git ignore rules
├── docs/
│   ├── mysql-init-clean.sql
│   └── mysql-schema.sql
├── crypto-core/                   # 核心支付/链上/配置/持久化能力
├── crypto-payment/                # 支付服务模块
├── crypto-watcher/                # Java 版本链上监听/扫描模块
├── crypto-manager/                # 管理后台/管理端服务
├── crypto-wallet/                 # 钱包相关模块, 空置
├── crypto-watcher-go/             # Go 版本链路监控模块
└──  crypto-secret-jni/            # Rust JNI 安全组件
```

## 模块说明

### crypto-core
核心业务库，提供：
- 支付订单域模型
- 链配置、网关配置、签名配置
- 交易状态解析
- 持久化映射与通用扩展能力

### crypto-payment
支付入口模块，负责：
- 订单创建与支付接口
- 网关/路由相关逻辑
- 回调处理
- 商户对接流程

### crypto-watcher
Java 版本的链上监听器，重点用于：
- 交易确认
- 事件扫描
- 链状态同步

### crypto-manager
管理端/后台模块，适合放置：
- 管理平台接口
- 审批/管理员操作
- 运营监控相关入口

### crypto-wallet
- 暂无实现

### crypto-watcher-go
Go 实现的 watcher 运行时，适合：
- 高性能异步处理
- 链上事件分发
- Redis/Kafka 之类的消息集成（视部署方式而定）

### crypto-secret-jni
Rust 编写的 JNI 安全组件，负责：
- AES-GCM 加解密
- 原生字符串/密钥处理
- 为 Java 层提供安全解密桥接

## 构建说明

### 1. Java / Maven

要求：
- JDK 25
- Maven 3.6.3+（建议使用较新版本）

构建入口：

```bash
mvn clean install
```

按模块构建：

```bash
mvn -pl crypto-core,crypto-payment,crypto-manager,crypto-wallet,crypto-watcher install
```

### 2. Go

```bash
cd crypto-watcher-go
go test ./...
go build ./...
```

### 3. Rust JNI

```bash
cd crypto-secret-jni
cargo test
cargo build --release
```

## 运行方式

Java 模块通常通过 Spring Boot 启动类启动，示例：

```bash
mvn spring-boot:run -pl crypto-manager
```

或直接运行对应 `mainClass`。

Go watcher 可以从目录启动：

```bash
cd crypto-watcher-go
go run ./cmd/watcher
```

## 配置建议

请注意以下内容不应提交到 GitHub：
- `.env` / `.env.*`
- `application-dev.yml`
- `application-local.yml`
- 私钥、证书、keystore
- 日志文件

项目已在根目录 `.gitignore` 中加入相关忽略规则。

## 数据库

`docs/` 目录中提供了：
- `mysql-schema.sql`
- `mysql-init-clean.sql`

可用于初始化本地数据库结构。

## 重要说明

- 当前项目已完成命名统一，根 Maven 父工程使用：`cryptopay-gateway-parent`
- Java 与 Go 的项目命名已按当前实现统一
- Rust JNI 中 JNI 导出方法名与 Java 类绑定是强关联关系，不能随意改动，否则会破坏 native 方法调用

## 维护建议

- 统一模块命名与 Maven 坐标
- 每个模块保持职责边界清晰
- 私钥、环境变量与本地配置统一放在本地，不提交到仓库
- IDE 的 `.idea` 和缓存文件不要当作源码的一部分提交

## 许可证

本项目当前未声明正式许可证，具体使用方式请以团队或业务方要求为准。
