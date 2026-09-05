package io.swzxsyh.payment.kyt;

import io.swzxsyh.payment.routing.WalletAccountType;
import java.math.BigDecimal;

/** KYT 风控请求。 */
public record KytScreeningRequest(
    String chain,
    String token,
    String tokenAddress,
    String merchantId,
    String cryptoOrderNo,
    String payerAddress,
    String payeeAddress,
    WalletAccountType walletAccountType,
    BigDecimal amount
) {
}
