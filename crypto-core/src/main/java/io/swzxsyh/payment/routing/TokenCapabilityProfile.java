package io.swzxsyh.payment.routing;

public record TokenCapabilityProfile(
    String chain,
    String token,
    String tokenAddress,
    int decimals,
    boolean transferWithAuthorization,
    boolean permit,
    boolean approve,
    boolean smartContractSettlement,
    String settlementContractAddress) {}
