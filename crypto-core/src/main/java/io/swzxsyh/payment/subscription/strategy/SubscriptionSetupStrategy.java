package io.swzxsyh.payment.subscription.strategy;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.model.SubscriptionSetupPlan;

/** 订阅准备策略接口，用于适配不同链和不同订阅协议。 */
public interface SubscriptionSetupStrategy {

  /** 返回当前策略所代表的订阅模式。 */
  SubscriptionBillingMode mode();

  /** 判断当前链和代币是否支持该策略。 */
  boolean supports(String chain, String token);

  /** 生成订阅准备阶段所需的执行计划。 */
  SubscriptionSetupPlan prepare(SubscriptionOrder order, CreateSubscriptionOrderRequest request);
}
