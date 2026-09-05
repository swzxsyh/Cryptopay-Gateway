package io.swzxsyh.payment.chain.status;

import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;

/** 链上交易状态解析上下文，后续可继续扩展 receipt、原始 log 或链特定字段。 */
public record ChainTransactionStatusContext(
    PendingChainTransaction pending,
    long latestBlock,
    int targetConfirmations) {

  public static ChainTransactionStatusContext of(PendingChainTransaction pending, long latestBlock) {
    int target = pending == null || pending.getTargetConfirmations() == null
        ? 1
        : Math.max(1, pending.getTargetConfirmations());
    return new ChainTransactionStatusContext(pending, latestBlock, target);
  }
}
