package io.swzxsyh.payment.exceptionorder;

/** 支付异常单处理状态。 */
public enum PaymentExceptionStatus {
  /** 待运营处理。 */
  PENDING,
  /** 处理中。 */
  PROCESSING,
  /** 已处理完成。 */
  RESOLVED,
  /** 已忽略或无需处理。 */
  IGNORED
}
