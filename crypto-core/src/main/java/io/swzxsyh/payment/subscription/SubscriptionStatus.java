package io.swzxsyh.payment.subscription;

/** 订阅订单状态枚举。 */
public enum SubscriptionStatus {
  /** 已创建，等待用户或平台完成链上订阅初始化。 */
  CREATED,
  /** 已提交链上初始化交易，等待确认。 */
  ACTIVATING,
  /** 订阅已生效。 */
  ACTIVE,
  /** 订阅已暂停。 */
  PAUSED,
  /** 订阅已取消。 */
  CANCELLED,
  /** 订阅已过期。 */
  EXPIRED,
  /** 订阅链上执行失败，等待人工或自动补偿。 */
  FAILED
}
