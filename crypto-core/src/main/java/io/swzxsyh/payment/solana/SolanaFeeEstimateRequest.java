package io.swzxsyh.payment.solana;

/** Solana 费用预估请求。 */
public record SolanaFeeEstimateRequest(
    String chain,
    String messageBase64,
    Integer signerCount
) {
}
