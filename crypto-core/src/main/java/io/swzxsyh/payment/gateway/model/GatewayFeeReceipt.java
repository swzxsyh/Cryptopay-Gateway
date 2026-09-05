package io.swzxsyh.payment.gateway.model;

import java.math.BigDecimal;

/** 网关服务费收据。 */
public record GatewayFeeReceipt(
    String receiptNo,
    String merchantId,
    String feeToken,
    BigDecimal feeAmount,
    BigDecimal remainingBalance
) {
}
