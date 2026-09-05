package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentDerivedAddressConfigMapper;
import io.swzxsyh.payment.util.SecretSourceType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 派生地址模式、HD/keystore/第三方钱包/地址工厂参数配置加载器。 */
@Component
@Order(120)
public class DerivedAddressConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentDerivedAddressConfigMapper derivedAddressMapper;

  public DerivedAddressConfigLoader(
      CryptoPaymentProperties properties,
      PaymentDerivedAddressConfigMapper derivedAddressMapper) {
    this.properties = properties;
    this.derivedAddressMapper = derivedAddressMapper;
  }

  @Override
  public void apply() {
    first(derivedAddressMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.DerivedAddress derivedAddress = properties.getDerivedAddress();
      setIfPresent(config.getDerivedEnabled(), derivedAddress::setEnabled);
      setIfPresent(config.getReuseAddress(), derivedAddress::setReuseAddress);
      setIfPresent(config.getReuseCooldownMinutes(), derivedAddress::setReuseCooldownMinutes);
      setIfText(config.getMode(), derivedAddress::setMode);
      setIfText(config.getHdMnemonicSourceType(), sourceType ->
          derivedAddress.getHd().setMnemonicSourceType(SecretSourceType.valueOf(sourceType.trim().toUpperCase())));
      setIfText(config.getHdMnemonicCiphertext(), derivedAddress.getHd()::setMnemonic);
      setIfText(config.getHdMnemonicKeyId(), derivedAddress.getHd()::setMnemonicKmsKeyId);
      setIfText(config.getHdMnemonicEnv(), derivedAddress.getHd()::setMnemonicEnv);
      setIfText(config.getHdDerivationPathPrefix(), derivedAddress.getHd()::setDerivationPathPrefix);
      setIfPresent(config.getHdStartIndex(), derivedAddress.getHd()::setStartIndex);
      setIfPresent(config.getHdGenerateMnemonicWhenMissing(), derivedAddress.getHd()::setGenerateMnemonicWhenMissing);
      setIfText(config.getKeystoreOutputDir(), derivedAddress.getKeystore()::setOutputDir);
      setIfText(config.getKeystorePasswordEnv(), derivedAddress.getKeystore()::setPasswordEnv);
      setIfText(config.getKeystoreFilePrefix(), derivedAddress.getKeystore()::setFilePrefix);
      setIfPresent(config.getThirdPartyEnabled(), derivedAddress.getThirdPartyApi()::setEnabled);
      setIfPresent(config.getThirdPartyBaseUrl(), derivedAddress.getThirdPartyApi()::setBaseUrl);
      setIfText(config.getThirdPartyCreatePath(), derivedAddress.getThirdPartyApi()::setCreatePath);
      setIfText(config.getThirdPartyApiKeyEnv(), derivedAddress.getThirdPartyApi()::setApiKeyEnv);
      setIfText(config.getThirdPartyProviderName(), derivedAddress.getThirdPartyApi()::setProviderName);
      setIfText(config.getAddressFactoryNamespace(), derivedAddress.getAddressFactory()::setNamespace);
      setIfText(config.getAddressFactorySeedPrefix(), derivedAddress.getAddressFactory()::setSeedPrefix);
    });
  }
}
