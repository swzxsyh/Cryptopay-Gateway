package io.swzxsyh.payment.subscription.strategy;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.contract.SubscriptionContractAdapterRegistry;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.model.SubscriptionSetupPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
/** Superfluid 流式订阅策略，保留协议编排入口。 */
public class SuperfluidStreamSubscriptionStrategy implements SubscriptionSetupStrategy {

  private final CryptoPaymentProperties properties;
  private final SubscriptionContractAdapterRegistry adapterRegistry;

  public SuperfluidStreamSubscriptionStrategy(
      CryptoPaymentProperties properties, SubscriptionContractAdapterRegistry adapterRegistry) {
    this.properties = properties;
    this.adapterRegistry = adapterRegistry;
  }

  @Override
  /** 返回当前策略对应的订阅模式。 */
  public SubscriptionBillingMode mode() {
    return SubscriptionBillingMode.SUPERFLUID_STREAM;
  }

  @Override
  /** 当前实现只依赖全局订阅开关。 */
  public boolean supports(String chain, String token) {
    return properties.getSubscription().isEnabled();
  }

  @Override
  /** 构造流式订阅准备计划。 */
  public SubscriptionSetupPlan prepare(SubscriptionOrder order, CreateSubscriptionOrderRequest request) {
    SubscriptionSetupPlan plan = adapterRegistry.get(mode()).prepareSetup(order, request);
    log.info("Prepared Superfluid stream plan. subscriptionOrderNo={}, chain={}, token={}, cfa={}",
        order.getSubscriptionOrderNo(), order.getChain(), order.getToken(), plan.setupContractAddress());
    return plan;
  }
}
