package io.swzxsyh.payment.domain;

/** 普通支付订单状态。 */
public enum OrderStatus {
  CREATED,
  METHOD_SELECTED,
  WAITING_PAYMENT,
  DETECTED,
  KYT_REVIEW,
  CONFIRMING,
  UNDERPAID,
  OVERPAID,
  PAID,
  EXPIRED,
  CANCELLED
}
