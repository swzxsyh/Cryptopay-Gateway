package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentSignatureConfigMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 入站验签、出站回调签名和重放保护配置加载器。 */
@Component
@Order(70)
public class SignatureConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentSignatureConfigMapper signatureMapper;

  public SignatureConfigLoader(
      CryptoPaymentProperties properties, PaymentSignatureConfigMapper signatureMapper) {
    this.properties = properties;
    this.signatureMapper = signatureMapper;
  }

  @Override
  public void apply() {
    first(signatureMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Signature signature = properties.getSignature();
      setIfPresent(config.getSignatureEnabled(), signature::setEnabled);
      setIfPresent(config.getVerifyInboundEnabled(), signature::setVerifyInboundEnabled);
      setIfPresent(config.getSignOutboundEnabled(), signature::setSignOutboundEnabled);
      setIfText(config.getAlgorithm(), signature::setAlgorithm);
      setIfText(config.getMerchantIdHeader(), signature::setMerchantIdHeader);
      setIfText(config.getTimestampHeader(), signature::setTimestampHeader);
      setIfText(config.getNonceHeader(), signature::setNonceHeader);
      setIfText(config.getSignatureHeader(), signature::setSignatureHeader);
      setIfText(config.getKeyVersionHeader(), signature::setKeyVersionHeader);
      setIfPresent(config.getAllowedClockSkewSeconds(), signature::setAllowedClockSkewSeconds);
      setIfPresent(config.getReplayTtlSeconds(), signature::setReplayTtlSeconds);
    });
  }
}
