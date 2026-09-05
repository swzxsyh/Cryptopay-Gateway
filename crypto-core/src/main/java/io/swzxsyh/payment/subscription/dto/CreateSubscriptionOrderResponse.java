package io.swzxsyh.payment.subscription.dto;

/** 订阅下单响应。 */
public record CreateSubscriptionOrderResponse(
    String subscriptionOrderNo,
    String subscriptionToken,
    String subscriptionUrl,
    SubscriptionOrderView order
) {
}
