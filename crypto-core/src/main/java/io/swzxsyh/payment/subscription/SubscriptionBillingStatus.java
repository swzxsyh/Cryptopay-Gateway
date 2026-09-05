package io.swzxsyh.payment.subscription;

/** 订阅周期账单状态。 */
public enum SubscriptionBillingStatus {
  /** 已生成账单，等待执行。 */
  PENDING,
  /** 已提交扣款交易，等待链上确认。 */
  EXECUTING,
  /** 链上已确认，但后置 KYT 需要人工复核；复核前不入商户余额、不回调成功。 */
  KYT_REVIEW,
  /** 扣款交易已确认。 */
  PAID,
  /** 本次扣款失败，等待重试或人工处理。 */
  FAILED,
  /** 账单已取消。 */
  CANCELLED
}
