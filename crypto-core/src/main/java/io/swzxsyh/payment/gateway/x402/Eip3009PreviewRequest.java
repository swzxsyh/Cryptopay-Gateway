package io.swzxsyh.payment.gateway.x402;

import java.math.BigDecimal;
import io.swzxsyh.payment.routing.WalletAccountType;

/** EIP-3009 预探测请求。 */
public record Eip3009PreviewRequest(
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType,
    String recipientAddress,
    BigDecimal amount
) {
}
