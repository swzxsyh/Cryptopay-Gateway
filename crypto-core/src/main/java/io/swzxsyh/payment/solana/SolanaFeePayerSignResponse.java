package io.swzxsyh.payment.solana;

/** Solana 平台 fee payer 签名响应。 */
public record SolanaFeePayerSignResponse(
    String chain,
    String feePayerAddress,
    String signatureBase64
) {}
