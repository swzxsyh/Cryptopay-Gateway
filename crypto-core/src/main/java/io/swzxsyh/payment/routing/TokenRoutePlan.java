package io.swzxsyh.payment.routing;

public record TokenRoutePlan(
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType,
    TokenRouteType routeType,
    String routeReason,
    boolean requiresUserAction,
    String payload
) {
}
