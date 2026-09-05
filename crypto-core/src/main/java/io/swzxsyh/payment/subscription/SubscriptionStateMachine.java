package io.swzxsyh.payment.subscription;

import org.springframework.stereotype.Component;

/** 订阅订单状态机，集中约束订阅状态只能按允许方向流转。 */
@Component
public class SubscriptionStateMachine {

  /** 校验并返回目标状态。 */
  public SubscriptionStatus transition(SubscriptionStatus current, SubscriptionStatus target) {
    if (current == target) {
      return target;
    }
    if (!canTransit(current, target)) {
      throw new IllegalStateException("Illegal subscription status transition: " + current + " -> " + target);
    }
    return target;
  }

  /** 判断状态是否允许流转。 */
  public boolean canTransit(SubscriptionStatus current, SubscriptionStatus target) {
    if (current == null || target == null) {
      return false;
    }
    return switch (current) {
      case CREATED -> target == SubscriptionStatus.ACTIVATING
          || target == SubscriptionStatus.ACTIVE
          || target == SubscriptionStatus.CANCELLED
          || target == SubscriptionStatus.FAILED;
      case ACTIVATING -> target == SubscriptionStatus.ACTIVE
          || target == SubscriptionStatus.CANCELLED
          || target == SubscriptionStatus.FAILED;
      case ACTIVE -> target == SubscriptionStatus.PAUSED
          || target == SubscriptionStatus.CANCELLED
          || target == SubscriptionStatus.EXPIRED
          || target == SubscriptionStatus.FAILED;
      case PAUSED -> target == SubscriptionStatus.ACTIVE
          || target == SubscriptionStatus.CANCELLED
          || target == SubscriptionStatus.EXPIRED;
      case FAILED -> target == SubscriptionStatus.ACTIVE
          || target == SubscriptionStatus.CANCELLED;
      case CANCELLED, EXPIRED -> false;
    };
  }
}
