package io.swzxsyh.payment.rawchain;

/** 原始链上流水处理状态。 */
public enum RawChainLogStatus {
  /** 已匹配到订单并进入支付处理链路。 */
  MATCHED,
  /** 未匹配到订单，留给运营排查或人工处理。 */
  UNMATCHED,
  /** 已由运营人工处理。 */
  MANUAL_PROCESSED
}
