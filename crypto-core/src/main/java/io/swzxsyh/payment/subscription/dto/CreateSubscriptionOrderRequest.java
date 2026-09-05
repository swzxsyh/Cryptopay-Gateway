package io.swzxsyh.payment.subscription.dto;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import java.math.BigDecimal;

/** 订阅下单请求。 */
public record CreateSubscriptionOrderRequest(
    String merchantOrderNo,
    String merchantId,
    BigDecimal amountPerCycle,
    String currency,
    String chain,
    String token,
    String tokenAddress,
    String payerAddress,
    String recipientAddress,
    SubscriptionBillingMode billingMode,
    Integer cycleSeconds,
    String notifyUrl,
    String returnUrl,
    String idempotencyKey) {}
