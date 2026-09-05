package io.swzxsyh.payment.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.PaymentGasConfigMapper;
import io.swzxsyh.payment.mapper.PaymentGasLowFeeChainMapper;
import io.swzxsyh.payment.persistence.entity.PaymentGasLowFeeChain;
import io.swzxsyh.payment.util.SecretSourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Gas 代付策略、防刷阈值和低费率链候选配置加载器。 */
@Slf4j
@Component
@Order(100)
public class GasConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentGasConfigMapper gasMapper;
  private final PaymentGasLowFeeChainMapper lowFeeChainMapper;

  public GasConfigLoader(
      CryptoPaymentProperties properties,
      PaymentGasConfigMapper gasMapper,
      PaymentGasLowFeeChainMapper lowFeeChainMapper) {
    this.properties = properties;
    this.gasMapper = gasMapper;
    this.lowFeeChainMapper = lowFeeChainMapper;
  }

  @Override
  public void apply() {
    first(gasMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Gas gas = properties.getGas();
      setIfPresent(config.getGasEnabled(), gas::setEnabled);
      setIfPresent(config.getHostedWalletPreferPlatform(), gas::setHostedWalletPreferPlatform);
      setIfPresent(config.getHostedWalletSponsorEnabled(), gas::setHostedWalletSponsorEnabled);
      setIfPresent(config.getEvmSponsorEnabled(), gas::setEvmSponsorEnabled);
      applySecretSource(config.getEvmRelayerPrivateKeySourceType(), gas);
      setIfText(config.getEvmRelayerPrivateKeyEnv(), gas::setEvmRelayerPrivateKeyEnv);
      setIfText(config.getEvmRelayerPrivateKeyKmsKeyId(), gas::setEvmRelayerPrivateKeyKmsKeyId);
      setIfPresent(config.getSponsorProtectionEnabled(), gas::setSponsorProtectionEnabled);
      setIfPresent(config.getSponsorCounterTtlSeconds(), gas::setSponsorCounterTtlSeconds);
      setIfPresent(config.getSponsorOrderMaxAttempts(), gas::setSponsorOrderMaxAttempts);
      setIfPresent(config.getSponsorWalletMaxAttempts(), gas::setSponsorWalletMaxAttempts);
      setIfPresent(config.getSponsorIpMaxAttempts(), gas::setSponsorIpMaxAttempts);
      setIfPresent(config.getSponsorCashierTokenMaxAttempts(), gas::setSponsorCashierTokenMaxAttempts);
      setIfPresent(config.getSponsorOrderLockLeaseSeconds(), gas::setSponsorOrderLockLeaseSeconds);
      setIfPresent(config.getCustomerBalancePrecheckEnabled(), gas::setCustomerBalancePrecheckEnabled);
      setIfPresent(config.getPlatformSponsoredGasLimit(), gas::setPlatformSponsoredGasLimit);
      setIfPresent(config.getCustomerGasLimit(), gas::setCustomerGasLimit);
      setIfPresent(config.getFreeTransferGasLimit(), gas::setFreeTransferGasLimit);
    });
    properties.getGas().setLowFeeChainCandidates(
        lowFeeChainMapper.selectList(Wrappers.<PaymentGasLowFeeChain>lambdaQuery()
                .eq(PaymentGasLowFeeChain::getEnabled, Boolean.TRUE)
                .orderByAsc(PaymentGasLowFeeChain::getSortNo))
            .stream()
            .map(PaymentGasLowFeeChain::getChainCode)
            .filter(StringUtils::hasText)
            .toList());
  }

  private void applySecretSource(String sourceType, CryptoPaymentProperties.Gas gas) {
    if (!StringUtils.hasText(sourceType)) {
      return;
    }
    try {
      gas.setEvmRelayerPrivateKeySourceType(SecretSourceType.valueOf(sourceType.trim().toUpperCase()));
    } catch (IllegalArgumentException ex) {
      log.warn("Unknown EVM relayer private key source type: {}", sourceType);
    }
  }
}
