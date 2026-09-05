package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentDiscoveryConfigMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 能力发现和 public base url 配置加载器。 */
@Component
@Order(60)
public class DiscoveryConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentDiscoveryConfigMapper discoveryMapper;

  public DiscoveryConfigLoader(
      CryptoPaymentProperties properties, PaymentDiscoveryConfigMapper discoveryMapper) {
    this.properties = properties;
    this.discoveryMapper = discoveryMapper;
  }

  @Override
  public void apply() {
    first(discoveryMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Discovery discovery = properties.getDiscovery();
      setIfPresent(config.getDiscoveryEnabled(), discovery::setEnabled);
      setIfText(config.getPublicBaseUrl(), discovery::setPublicBaseUrl);
    });
  }
}
