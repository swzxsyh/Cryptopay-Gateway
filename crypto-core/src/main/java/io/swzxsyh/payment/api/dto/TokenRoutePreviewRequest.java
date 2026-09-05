package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.routing.WalletAccountType;

/** 支付路由预探测请求。 */
public record TokenRoutePreviewRequest(
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType
) {
}
