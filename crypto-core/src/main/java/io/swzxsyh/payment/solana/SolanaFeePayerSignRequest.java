package io.swzxsyh.payment.solana;

/** Solana 平台 fee payer 对交易 message 做签名的请求。 */
public record SolanaFeePayerSignRequest(
    String chain,
    String messageBase64
) {}
