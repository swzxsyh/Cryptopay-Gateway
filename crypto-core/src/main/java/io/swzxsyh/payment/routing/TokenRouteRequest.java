package io.swzxsyh.payment.routing;

public record TokenRouteRequest(
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType
) {
}
