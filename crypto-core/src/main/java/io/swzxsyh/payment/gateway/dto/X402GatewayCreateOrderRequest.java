package io.swzxsyh.payment.gateway.dto;

import java.math.BigDecimal;

/** X402 网关下单请求。 */
public record X402GatewayCreateOrderRequest(
    String merchantId,
    String merchantOrderNo,
    BigDecimal amount,
    String currency,
    String notifyUrl,
    String returnUrl,
    String idempotencyKey
) {
}
