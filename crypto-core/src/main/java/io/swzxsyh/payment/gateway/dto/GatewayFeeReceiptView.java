package io.swzxsyh.payment.gateway.dto;

import java.math.BigDecimal;

/** 网关服务费收据视图。 */
public record GatewayFeeReceiptView(
    String receiptNo,
    String merchantId,
    String feeToken,
    BigDecimal feeAmount,
    BigDecimal remainingBalance
) {
}
