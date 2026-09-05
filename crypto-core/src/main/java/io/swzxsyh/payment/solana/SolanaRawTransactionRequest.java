package io.swzxsyh.payment.solana;

/** Solana raw transaction 广播请求。 */
public record SolanaRawTransactionRequest(
    String chain,
    String signedTransactionBase64
) {
}
