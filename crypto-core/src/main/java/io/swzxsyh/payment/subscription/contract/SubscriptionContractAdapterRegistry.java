package io.swzxsyh.payment.subscription.contract;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 订阅合约适配器注册表。 */
@Component
public class SubscriptionContractAdapterRegistry {

  private final Map<SubscriptionBillingMode, SubscriptionContractAdapter> adapters =
      new EnumMap<>(SubscriptionBillingMode.class);

  public SubscriptionContractAdapterRegistry(List<SubscriptionContractAdapter> adapters) {
    for (SubscriptionContractAdapter adapter : adapters) {
      this.adapters.put(adapter.mode(), adapter);
    }
  }

  /** 获取指定订阅类型的合约适配器。 */
  public SubscriptionContractAdapter get(SubscriptionBillingMode mode) {
    SubscriptionContractAdapter adapter = adapters.get(mode);
    if (adapter == null) {
      throw new IllegalStateException("No subscription contract adapter configured for mode: " + mode);
    }
    return adapter;
  }
}
