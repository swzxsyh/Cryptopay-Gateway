package io.swzxsyh.payment.solana;

import java.math.BigDecimal;

/** Solana 费用预估响应，lamports 是 Solana 原生最小单位。 */
public record SolanaFeeEstimateResponse(
    String chain,
    long feeLamports,
    BigDecimal feeSol,
    boolean estimatedByRpc,
    String blockhash,
    long lastValidBlockHeight,
    String reason
) {
}
