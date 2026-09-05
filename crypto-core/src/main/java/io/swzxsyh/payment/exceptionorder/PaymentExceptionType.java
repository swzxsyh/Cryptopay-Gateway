package io.swzxsyh.payment.exceptionorder;

/** 支付异常单类型。 */
public enum PaymentExceptionType {
  /** 订单过期后才监听到链上入账。 */
  LATE_PAYMENT,
  /** 链上入账后 KYT 要求人工复核。 */
  KYT_REVIEW,
  /** 链上入账后 KYT 判定拒绝或高危。 */
  KYT_REJECTED
}
