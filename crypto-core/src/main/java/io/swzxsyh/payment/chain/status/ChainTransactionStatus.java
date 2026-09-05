package io.swzxsyh.payment.chain.status;

/** 链上交易统一状态，屏蔽不同链的 receipt、slot、checkpoint 等差异。 */
public enum ChainTransactionStatus {
  /** 已在链上发现，但还不能确认是否稳定。 */
  OBSERVED,
  /** 链上仍在确认中。 */
  CONFIRMING,
  /** 已达到业务确认条件，可以交给 payment 入账链路。 */
  CONFIRMED,
  /** 链上执行失败。 */
  FAILED,
  /** 交易或日志不再位于规范链上，通常由 reorg 或节点回滚导致。 */
  REORGED,
  /** 长时间查不到或被链/节点丢弃。 */
  DROPPED,
  /** 当前链还没有接入状态解析能力。 */
  UNSUPPORTED
}
