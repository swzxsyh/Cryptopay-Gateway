package io.swzxsyh.payment.subscription.model;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;

/** 订阅准备阶段的执行计划，供后续协议适配器或前端展示使用。 */
public record SubscriptionSetupPlan(
    SubscriptionBillingMode billingMode,
    String setupContractAddress,
    String setupPayload,
    String setupReason
) {
}
