package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentCashierConfigMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 收银台 token 加密、短链和过期时间配置加载器。 */
@Component
@Order(80)
public class CashierConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentCashierConfigMapper cashierMapper;

  public CashierConfigLoader(
      CryptoPaymentProperties properties, PaymentCashierConfigMapper cashierMapper) {
    this.properties = properties;
    this.cashierMapper = cashierMapper;
  }

  @Override
  public void apply() {
    first(cashierMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Cashier cashier = properties.getCashier();
      setIfPresent(config.getTokenEncryptionEnabled(), cashier::setTokenEncryptionEnabled);
      setIfText(config.getTokenKeyAlias(), cashier::setTokenKeyAlias);
      setIfPresent(config.getTokenTtlMinutes(), cashier::setTokenTtlMinutes);
      setIfText(config.getTokenPrefix(), cashier::setTokenPrefix);
      setIfPresent(config.getShortTokenEnabled(), cashier::setShortTokenEnabled);
      setIfText(config.getShortTokenPrefix(), cashier::setShortTokenPrefix);
      setIfPresent(config.getShortTokenLength(), cashier::setShortTokenLength);
    });
  }
}
