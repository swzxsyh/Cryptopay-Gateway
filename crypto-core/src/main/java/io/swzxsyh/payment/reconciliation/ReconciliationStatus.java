package io.swzxsyh.payment.reconciliation;

/** 对账结果状态。 */
public enum ReconciliationStatus {
  MATCHED,
  WARNING,
  MISMATCH,
  MANUAL_CONFIRMED
}
