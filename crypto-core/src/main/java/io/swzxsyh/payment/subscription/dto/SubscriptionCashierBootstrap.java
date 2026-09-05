package io.swzxsyh.payment.subscription.dto;

/** 订阅收银台启动数据。 */
public record SubscriptionCashierBootstrap(
    SubscriptionOrderView order,
    SubscriptionBillingRecordView latestBilling,
    String contractType,
    String setupContractAddress,
    String setupCallData,
    String setupReason
) {}
