package io.swzxsyh.payment.sponsor;

import java.math.BigInteger;

/** EIP-3009 transferWithAuthorization 的用户签名提交参数。 */
public record Eip3009AuthorizationSubmission(
    String chain,
    String tokenAddress,
    String from,
    String to,
    BigInteger value,
    BigInteger validAfter,
    BigInteger validBefore,
    String nonce,
    Integer v,
    String r,
    String s
) {
}
