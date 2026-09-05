package io.swzxsyh.payment.domain;

import io.swzxsyh.payment.routing.WalletAccountType;

/** 用户选择支付方式后的最终路由输入。 */
public record PaymentSelection(
    PaymentMethod paymentMethod,
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType
) {
}
