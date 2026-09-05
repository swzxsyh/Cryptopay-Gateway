package io.swzxsyh.payment.settlement.record;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ContractSettlementRecordView(
    String cryptoOrderNo,
    String merchantOrderNo,
    String chain,
    String token,
    String tokenAddress,
    String contractAddress,
    BigDecimal amount,
    String status,
    String settlementMode,
    Integer splitCount,
    Integer totalBasisPoints,
    String signPayload,
    String signature,
    String contractCallData,
    String paymentTxHash,
    Long blockNumber,
    LocalDateTime plannedAt,
    LocalDateTime submittedAt,
    LocalDateTime settledAt,
    LocalDateTime failedAt,
    String failureReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ContractSettlementSplitView> splits) {}
