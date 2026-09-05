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
/** ERC-1337 订阅策略，预留标准化订阅协议适配点。 */
public class Erc1337SubscriptionStrategy implements SubscriptionSetupStrategy {

  private final CryptoPaymentProperties properties;
  private final SubscriptionContractAdapterRegistry adapterRegistry;

  public Erc1337SubscriptionStrategy(
      CryptoPaymentProperties properties, SubscriptionContractAdapterRegistry adapterRegistry) {
    this.properties = properties;
    this.adapterRegistry = adapterRegistry;
  }

  /** 返回当前策略对应的订阅模式。 */
  @Override
  public SubscriptionBillingMode mode() {
    return SubscriptionBillingMode.ERC_1337;
  }

  /** 当前实现只依赖全局订阅开关。 */
  @Override
  public boolean supports(String chain, String token) {
    return properties.getSubscription().isEnabled();
  }

  /** 构造 ERC-1337 风格的订阅准备计划。 */
  @Override
  public SubscriptionSetupPlan prepare(SubscriptionOrder order, CreateSubscriptionOrderRequest request) {
    SubscriptionSetupPlan plan = adapterRegistry.get(mode()).prepareSetup(order, request);
    log.info("Prepared ERC-1337 subscription plan. subscriptionOrderNo={}, chain={}, token={}, executor={}",
        order.getSubscriptionOrderNo(), order.getChain(), order.getToken(), plan.setupContractAddress());
    return plan;
  }
}
