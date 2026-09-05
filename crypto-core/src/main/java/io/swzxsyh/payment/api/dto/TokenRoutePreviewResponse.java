package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.routing.TokenRoutePlan;
import io.swzxsyh.payment.routing.TokenRouteType;
import io.swzxsyh.payment.routing.WalletAccountType;

/** 支付路由预探测结果。 */
public record TokenRoutePreviewResponse(
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

  public static TokenRoutePreviewResponse from(TokenRoutePlan plan) {
    return new TokenRoutePreviewResponse(
        plan.chain(),
        plan.token(),
        plan.tokenAddress(),
        plan.walletAddress(),
        plan.walletAccountType(),
        plan.routeType(),
        plan.routeReason(),
        plan.requiresUserAction(),
        plan.payload()
    );
  }
}
