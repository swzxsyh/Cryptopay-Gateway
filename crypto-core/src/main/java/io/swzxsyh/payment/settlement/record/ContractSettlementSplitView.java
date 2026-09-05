package io.swzxsyh.payment.settlement.record;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContractSettlementSplitView(
    String role,
    String receiver,
    Integer basisPoints,
    BigDecimal amount,
    String status,
    String paymentTxHash,
    Long blockNumber,
    LocalDateTime executedAt) {}
