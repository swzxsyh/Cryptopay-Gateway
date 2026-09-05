package io.swzxsyh.payment.gateway.dto;

import io.swzxsyh.payment.api.dto.CreateCryptoOrderResponse;

/** X402 网关下单响应。 */
public record X402GatewayCreateOrderResponse(
    GatewayFeeReceiptView feeReceipt,
    CreateCryptoOrderResponse order
) {
}
