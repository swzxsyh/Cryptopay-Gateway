package io.swzxsyh.payment.subscription.dto;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订阅订单展示视图。 */
public record SubscriptionOrderView(
    String subscriptionOrderNo,
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
    int cycleSeconds,
    BigDecimal transactionFeeRate,
    BigDecimal minimumFee,
    BigDecimal fixedFee,
    BigDecimal gatewayFee,
    BigDecimal taxRate,
    String feeSettlementMode,
    String notifyUrl,
    String returnUrl,
    String setupContractAddress,
    String setupPayload,
    String setupReason,
    String setupTxHash,
    String subscriptionEventId,
    SubscriptionStatus status,
    LocalDateTime nextBillingAt,
    LocalDateTime lastBillingAt,
    LocalDateTime activatedAt,
    LocalDateTime pausedAt,
    LocalDateTime cancelledAt,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
  public static SubscriptionOrderView from(SubscriptionOrder order) {
    return new SubscriptionOrderView(
        order.getSubscriptionOrderNo(),
        order.getMerchantOrderNo(),
        order.getMerchantId(),
        order.getAmountPerCycle(),
        order.getCurrency(),
        order.getChain(),
        order.getToken(),
        order.getTokenAddress(),
        order.getPayerAddress(),
        order.getRecipientAddress(),
        order.getBillingMode(),
        order.getCycleSeconds(),
        order.getTransactionFeeRate(),
        order.getMinimumFee(),
        order.getFixedFee(),
        order.getGatewayFee(),
        order.getTaxRate(),
        order.getFeeSettlementMode(),
        order.getNotifyUrl(),
        order.getReturnUrl(),
        order.getSetupContractAddress(),
        order.getSetupPayload(),
        order.getSetupReason(),
        order.getSetupTxHash(),
        order.getSubscriptionEventId(),
        order.getStatus(),
        order.getNextBillingAt(),
        order.getLastBillingAt(),
        order.getActivatedAt(),
        order.getPausedAt(),
        order.getCancelledAt(),
        order.getFailureReason(),
        order.getCreatedAt(),
        order.getUpdatedAt()
    );
  }
}
