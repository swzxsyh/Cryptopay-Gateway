package io.swzxsyh.payment.api.dto;

/** 收银台可选链路。 */
public record CashierChainOption(
    String chain,
    String family,
    String token,
    String tokenAddress,
    Integer tokenDecimals,
    String rpcUrl
) {
}
