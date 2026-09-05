package io.swzxsyh.watcher.dto;

import java.math.BigDecimal;

/** 链上支付通知的统一记录结构。 */
public record PaymentNotificationRecord(
    String chain,
    String cryptoOrderNo,
    String merchantId,
    String merchantOrderNo,
    String txHash,
    String fromAddress,
    String toAddress,
    BigDecimal amount,
    BigDecimal realAmount,
    Long blockNumber,
    BigDecimal transactionFee,
    BigDecimal taxFee,
    BigDecimal totalFee,
    BigDecimal settlementAmount,
    boolean latePayment,
    String orderStatus
) {}
