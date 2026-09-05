package io.swzxsyh.payment.subscription.dto;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;

/** 订阅生命周期状态变更回调负载。 */
public record SubscriptionStatusNotificationRecord(
    String subscriptionOrderNo,
    String merchantOrderNo,
    String merchantId,
    String chain,
    String token,
    String eventType,
    String previousStatus,
    String currentStatus,
    SubscriptionBillingMode billingMode,
    String txHash,
    String reason
) {}
