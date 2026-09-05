package io.swzxsyh.payment.subscription.dto;

/** 订阅动作响应。 */
public record SubscriptionActionResponse(
    String subscriptionOrderNo,
    String status,
    boolean submitted,
    String txHash,
    String message
) {}
