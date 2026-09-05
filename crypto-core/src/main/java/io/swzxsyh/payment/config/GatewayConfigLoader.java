package io.swzxsyh.payment.config;

import io.swzxsyh.payment.mapper.PaymentGatewayConfigMapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** x402 网关与服务费门禁配置加载器。 */
@Component
@Order(50)
public class GatewayConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentGatewayConfigMapper gatewayMapper;

  public GatewayConfigLoader(
      CryptoPaymentProperties properties, PaymentGatewayConfigMapper gatewayMapper) {
    this.properties = properties;
    this.gatewayMapper = gatewayMapper;
  }

  @Override
  public void apply() {
    first(gatewayMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Gateway gateway = properties.getGateway();
      setIfPresent(config.getGatewayEnabled(), gateway::setEnabled);
      setIfText(config.getServiceFeeToken(), gateway::setServiceFeeToken);
      setIfPresent(config.getServiceFeeAmount(), gateway::setServiceFeeAmount);
      setIfPresent(config.getApiKey(), gateway::setApiKey);
      setIfPresent(config.getPublicResourcesEnabled(), gateway::setPublicResourcesEnabled);
      setIfText(config.getFacilitatorName(), gateway::setFacilitatorName);
    });
  }
}
