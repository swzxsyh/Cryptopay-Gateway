package io.swzxsyh.payment.messaging;

import java.time.Instant;

/**
 * 订阅合约链上事件的统一消息体。
 *
 * <p>Watcher 只负责解析链上日志并发布该消息；payment 侧消费后再进入订阅状态机、账单确认、
 * KYT、记账和回调流程。
 */
public record SubscriptionChainEvent(
    String eventId,
    String source,
    String chain,
    String contract,
    String eventType,
    String subscriptionId,
    String txHash,
    Long logIndex,
    Long blockNumber,
    Instant blockTimestamp,
    String sequence,
    String amountRaw,
    Instant observedAt) {}
