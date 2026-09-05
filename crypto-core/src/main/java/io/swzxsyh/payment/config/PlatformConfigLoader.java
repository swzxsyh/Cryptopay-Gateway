package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentPlatformConfigMapper;
import io.swzxsyh.payment.persistence.entity.PaymentPlatformConfig;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 平台基础配置加载器。 */
@Component
@Order(10)
public class PlatformConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentPlatformConfigMapper platformMapper;

  public PlatformConfigLoader(
      CryptoPaymentProperties properties, PaymentPlatformConfigMapper platformMapper) {
    this.properties = properties;
    this.platformMapper = platformMapper;
  }

  @Override
  public void apply() {
    firstEnabled(platformMapper.selectList(null), PaymentPlatformConfig::getEnabled)
        .ifPresent(
            config -> {
              setIfPresent(config.getOrderExpireMinutes(), properties::setOrderExpireMinutes);
              setIfText(config.getCashierBaseUrl(), properties::setCashierBaseUrl);
              setIfText(config.getTreasuryAddress(), properties::setTreasuryAddress);
              setIfPresent(config.getIdempotencyWaitMillis(), properties::setIdempotencyWaitMillis);
            });
  }
}
