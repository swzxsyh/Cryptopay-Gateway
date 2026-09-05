package io.swzxsyh.payment.domain;

import io.swzxsyh.payment.routing.TokenRouteType;
import io.swzxsyh.payment.routing.WalletAccountType;

/** 支付方式编排后的结果。 */
public record PaymentDetails(
    PaymentMethod paymentMethod,
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType,
    TokenRouteType tokenRouteType,
    String routeReason,
    String paymentAddress,
    String contractAddress,
    String contractCallData,
    String gasPayerMode,
    String gasReason,
    java.math.BigDecimal gasEstimatedFeeWei,
    boolean gasCustomerBalanceSufficient,
    boolean gasPlatformBalanceSufficient,
    String gasFallbackSuggestion,
    String derivedAddressPoolKey,
    String derivedAddressLeaseId
) {
}
