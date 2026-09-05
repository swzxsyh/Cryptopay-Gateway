package io.swzxsyh.payment.subscription.dto;

import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionBillingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订阅周期账单视图。 */
public record SubscriptionBillingRecordView(
    Long id,
    String subscriptionOrderNo,
    Integer billingSequence,
    BigDecimal amount,
    BigDecimal realAmount,
    String currency,
    String chain,
    String token,
    BigDecimal transactionFee,
    BigDecimal taxFee,
    BigDecimal totalFee,
    BigDecimal settlementAmount,
    String feeSettlementMode,
    SubscriptionBillingStatus status,
    String executionTxHash,
    Long confirmedBlockNumber,
    LocalDateTime dueAt,
    LocalDateTime paidAt,
    Integer retryCount,
    String failureReason
) {

  public static SubscriptionBillingRecordView from(SubscriptionBillingRecord record) {
    return new SubscriptionBillingRecordView(
        record.getId(),
        record.getSubscriptionOrderNo(),
        record.getBillingSequence(),
        record.getAmount(),
        record.getRealAmount(),
        record.getCurrency(),
        record.getChain(),
        record.getToken(),
        record.getTransactionFee(),
        record.getTaxFee(),
        record.getTotalFee(),
        record.getSettlementAmount(),
        record.getFeeSettlementMode(),
        record.getStatus(),
        record.getExecutionTxHash(),
        record.getConfirmedBlockNumber(),
        record.getDueAt(),
        record.getPaidAt(),
        record.getRetryCount(),
        record.getFailureReason());
  }
}
