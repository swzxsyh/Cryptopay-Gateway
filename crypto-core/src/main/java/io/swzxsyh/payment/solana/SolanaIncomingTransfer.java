package io.swzxsyh.payment.solana;

import java.math.BigDecimal;
import java.time.Instant;

/** Solana 入账解析结果。 */
public record SolanaIncomingTransfer(
    String signature,
    long slot,
    String sourceAddress,
    String destinationAddress,
    String tokenAddress,
    BigDecimal amount,
    Instant blockTimestamp,
    boolean nativeTransfer,
    int decimals) {}
