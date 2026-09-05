package io.swzxsyh.payment.gateway.x402;

/** EIP-3009 预探测结果。 */
public record Eip3009PreviewResponse(
    boolean supported,
    String routeType,
    String routeReason,
    boolean requiresUserAction,
    String token,
    String tokenAddress,
    Eip3009AuthorizationTemplate authorization
) {
}
