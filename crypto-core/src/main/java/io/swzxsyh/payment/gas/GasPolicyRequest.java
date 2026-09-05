package io.swzxsyh.payment.gas;

import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.routing.TokenRouteType;
import io.swzxsyh.payment.routing.WalletAccountType;
import java.math.BigDecimal;

public record GasPolicyRequest(
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType,
    PaymentMethod paymentMethod,
    TokenRouteType tokenRouteType,
    BigDecimal amount) {}
