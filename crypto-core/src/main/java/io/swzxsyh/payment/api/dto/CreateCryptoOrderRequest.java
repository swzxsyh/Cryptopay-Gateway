package io.swzxsyh.payment.api.dto;

import java.math.BigDecimal;

/** 普通下单请求。 */
public record CreateCryptoOrderRequest(
    String merchantId,
    String merchantOrderNo,
    BigDecimal amount,
    String currency,
    String notifyUrl,
    String returnUrl,
    String idempotencyKey
) {
}
