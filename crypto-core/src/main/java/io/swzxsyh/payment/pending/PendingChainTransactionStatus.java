package io.swzxsyh.payment.pending;

/** 未确认链上交易的旁路状态，不直接代表订单资金最终状态。 */
public enum PendingChainTransactionStatus {
  /** 已提前发现，等待确认数推进。 */
  PENDING,
  /** 已达到确认数并投递给现有入账处理链路。 */
  DISPATCHED,
  /** 已被最终链上入账链路匹配确认。 */
  MATCHED,
  /** 已由运营或系统取消跟踪。 */
  CANCELLED
}
