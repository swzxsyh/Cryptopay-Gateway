package io.swzxsyh.payment.solana;

/** Solana raw transaction 广播响应。 */
public record SolanaRawTransactionResponse(
    String chain,
    String signature
) {
}
