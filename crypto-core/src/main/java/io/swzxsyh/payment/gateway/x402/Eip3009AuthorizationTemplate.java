package io.swzxsyh.payment.gateway.x402;

import java.math.BigDecimal;

/** EIP-3009 授权模板。 */
public record Eip3009AuthorizationTemplate(
    String chain,
    String token,
    String tokenAddress,
    String fromAddress,
    String toAddress,
    BigDecimal amount,
    int decimals,
    Long validAfter,
    Long validBefore,
    String nonce,
    String domain,
    String message,
    String payload
) {
}
