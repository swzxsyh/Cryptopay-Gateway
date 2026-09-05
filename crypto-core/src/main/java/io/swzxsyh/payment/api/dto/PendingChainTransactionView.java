package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;
import java.time.LocalDateTime;

/** 收银台展示用的交易确认进度。 */
public record PendingChainTransactionView(
    String txHash,
    Long blockNumber,
    Integer targetConfirmations,
    Integer currentConfirmations,
    String confirmationStatus,
    LocalDateTime observedAt
) {

  public static PendingChainTransactionView from(PendingChainTransaction pending) {
    if (pending == null) {
      return null;
    }
    return new PendingChainTransactionView(
        pending.getTxHash(),
        pending.getBlockNumber(),
        pending.getTargetConfirmations(),
        pending.getCurrentConfirmations(),
        pending.getStatus(),
        pending.getObservedAt());
  }
}
