package io.swzxsyh.payment.api.dto;

/** 普通下单响应。 */
public record CreateCryptoOrderResponse(
    String cryptoOrderNo,
    String cashierToken,
    String cashierUrl,
    MerchantPaymentOrderView order
) {
}
