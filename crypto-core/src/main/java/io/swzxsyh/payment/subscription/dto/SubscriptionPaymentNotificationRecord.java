package io.swzxsyh.payment.subscription.dto;

import java.math.BigDecimal;

/** 订阅周期扣款回调负载。 */
public record SubscriptionPaymentNotificationRecord(
    String subscriptionOrderNo,
    String merchantOrderNo,
    String merchantId,
    Integer billingSequence,
    String chain,
    String token,
    String txHash,
    BigDecimal amount,
    BigDecimal realAmount,
    Long blockNumber,
    BigDecimal transactionFee,
    BigDecimal taxFee,
    BigDecimal totalFee,
    BigDecimal settlementAmount,
    String subscriptionStatus,
    String billingStatus
) {}
