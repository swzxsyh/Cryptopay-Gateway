package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.routing.WalletAccountType;

/** 选择支付方式请求。 */
public record SelectPaymentMethodRequest(
    PaymentMethod paymentMethod,
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType
) {
}
