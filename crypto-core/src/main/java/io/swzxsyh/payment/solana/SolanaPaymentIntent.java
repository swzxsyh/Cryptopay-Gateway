package io.swzxsyh.payment.solana;

import java.math.BigDecimal;

/** Solana 钱包支付意图，前端据此构造 Phantom/Solflare 等钱包交易。 */
public record SolanaPaymentIntent(
    String cryptoOrderNo,
    String chain,
    String rpcUrl,
    String recipient,
    BigDecimal amount,
    String token,
    String tokenAddress,
    int tokenDecimals,
    boolean feePayerEnabled,
    String feePayerMode,
    String feePayerAddress,
    long estimatedFeeLamports,
    BigDecimal estimatedFeeSol,
    String blockhash,
    long lastValidBlockHeight,
    String note
) {
}
