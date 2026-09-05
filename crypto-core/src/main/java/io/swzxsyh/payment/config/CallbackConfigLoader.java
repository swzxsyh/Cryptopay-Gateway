package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentCallbackConfigMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 商户回调重试、死信和保留时间配置加载器。 */
@Component
@Order(140)
public class CallbackConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentCallbackConfigMapper callbackMapper;

  public CallbackConfigLoader(
      CryptoPaymentProperties properties, PaymentCallbackConfigMapper callbackMapper) {
    this.properties = properties;
    this.callbackMapper = callbackMapper;
  }

  @Override
  public void apply() {
    first(callbackMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Callback callback = properties.getCallback();
      setIfPresent(config.getRetryEnabled(), callback::setRetryEnabled);
      setIfPresent(config.getMaxAttempts(), callback::setMaxAttempts);
      setIfPresent(config.getInitialBackoffSeconds(), callback::setInitialBackoffSeconds);
      setIfPresent(config.getMaxBackoffSeconds(), callback::setMaxBackoffSeconds);
      setIfPresent(config.getRetentionDays(), callback::setRetentionDays);
      setIfPresent(config.getDeadLetterEnabled(), callback::setDeadLetterEnabled);
      setIfPresent(config.getDispatchImmediately(), callback::setDispatchImmediately);
      setIfPresent(config.getDispatchBatchSize(), callback::setDispatchBatchSize);
      setIfPresent(config.getDispatchLockWaitMillis(), callback::setDispatchLockWaitMillis);
      setIfPresent(config.getDispatchLockLeaseSeconds(), callback::setDispatchLockLeaseSeconds);
      setIfPresent(config.getMaxStoredResponseChars(), callback::setMaxStoredResponseChars);
      setIfPresent(config.getMaxStoredErrorChars(), callback::setMaxStoredErrorChars);
    });
  }
}
