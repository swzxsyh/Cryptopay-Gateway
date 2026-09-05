package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentSubscriptionConfigMapper;
import io.swzxsyh.payment.util.SecretSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 订阅支付模式、执行合约和执行器密钥来源配置加载器。 */
@Slf4j
@Component
@Order(90)
public class SubscriptionConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentSubscriptionConfigMapper subscriptionMapper;

  public SubscriptionConfigLoader(
      CryptoPaymentProperties properties, PaymentSubscriptionConfigMapper subscriptionMapper) {
    this.properties = properties;
    this.subscriptionMapper = subscriptionMapper;
  }

  @Override
  public void apply() {
    first(subscriptionMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Subscription subscription = properties.getSubscription();
      setIfPresent(config.getSubscriptionEnabled(), subscription::setEnabled);
      setIfText(config.getDefaultMode(), subscription::setDefaultMode);
      setIfPresent(config.getDefaultCycleSeconds(), subscription::setDefaultCycleSeconds);
      setIfPresent(config.getSuperfluidHostAddress(), subscription::setSuperfluidHostAddress);
      setIfPresent(config.getSuperfluidCfaAddress(), subscription::setSuperfluidCfaAddress);
      setIfPresent(config.getErc1337ExecutorAddress(), subscription::setErc1337ExecutorAddress);
      setIfPresent(config.getSchedulerEnabled(), subscription::setSchedulerEnabled);
      setIfPresent(config.getSchedulerIntervalSeconds(), subscription::setSchedulerIntervalSeconds);
      setIfPresent(config.getStreamSettlementIntervalSeconds(), subscription::setStreamSettlementIntervalSeconds);
      setIfPresent(config.getSchedulerBatchSize(), subscription::setSchedulerBatchSize);
      setIfPresent(config.getMaxRetryCount(), subscription::setMaxRetryCount);
      setIfPresent(config.getRetryBackoffSeconds(), subscription::setRetryBackoffSeconds);
      setIfPresent(config.getExecutionGasLimit(), subscription::setExecutionGasLimit);
      applySecretSource(config.getExecutorPrivateKeySourceType(), subscription);
      setIfText(config.getExecutorPrivateKeyEnv(), subscription::setExecutorPrivateKeyEnv);
      setIfText(config.getExecutorPrivateKeyKmsKeyId(), subscription::setExecutorPrivateKeyKmsKeyId);
    });
  }

  private void applySecretSource(
      String sourceType, CryptoPaymentProperties.Subscription subscription) {
    if (!StringUtils.hasText(sourceType)) {
      return;
    }
    try {
      subscription.setExecutorPrivateKeySourceType(
          SecretSourceType.valueOf(sourceType.trim().toUpperCase()));
    } catch (IllegalArgumentException ex) {
      log.warn("Unknown subscription executor private key source type: {}", sourceType);
    }
  }
}
