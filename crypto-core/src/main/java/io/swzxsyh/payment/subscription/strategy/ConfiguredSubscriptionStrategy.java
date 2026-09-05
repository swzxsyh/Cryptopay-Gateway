package io.swzxsyh.payment.subscription.strategy;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.model.SubscriptionSetupPlan;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 订阅编排策略入口，根据配置和订单能力选择具体订阅实现。 */
@Slf4j
@Component
public class ConfiguredSubscriptionStrategy {

  private final CryptoPaymentProperties properties;
  private final ObjectProvider<List<SubscriptionSetupStrategy>> strategiesProvider;

  public ConfiguredSubscriptionStrategy(
      CryptoPaymentProperties properties,
      ObjectProvider<List<SubscriptionSetupStrategy>> strategiesProvider) {
    this.properties = properties;
    this.strategiesProvider = strategiesProvider;
  }

  /** 根据订单和请求选择一个可用的订阅准备策略。 */
  public SubscriptionSetupPlan prepare(
      SubscriptionOrder order, CreateSubscriptionOrderRequest request) {
    SubscriptionBillingMode requestedMode = resolveMode(request.billingMode());
    List<SubscriptionSetupStrategy> strategies = strategiesProvider.getIfAvailable(List::of);
    for (SubscriptionSetupStrategy strategy : strategies) {
      if (strategy.mode() == requestedMode
          && strategy.supports(order.getChain(), order.getToken())) {
        return strategy.prepare(order, request);
      }
    }

    throw new IllegalStateException(
        "No subscription strategy configured for mode: " + requestedMode);
  }

  /** 解析订阅默认模式，优先使用请求传入值，其次使用配置值。 */
  private SubscriptionBillingMode resolveMode(SubscriptionBillingMode requestMode) {
    if (requestMode != null) {
      return requestMode;
    }
    String configured = properties.getSubscription().getDefaultMode();
    if (!StringUtils.hasText(configured)) {
      return SubscriptionBillingMode.SUPERFLUID_STREAM;
    }
    try {
      return SubscriptionBillingMode.valueOf(configured.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      log.warn("Unknown subscription default mode {}, fallback to SUPERFLUID_STREAM", configured);
      return SubscriptionBillingMode.SUPERFLUID_STREAM;
    }
  }
}
