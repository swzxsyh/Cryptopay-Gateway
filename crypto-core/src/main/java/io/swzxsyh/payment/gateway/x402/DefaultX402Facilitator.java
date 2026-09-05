package io.swzxsyh.payment.gateway.x402;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderRequest;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderResponse;
import io.swzxsyh.payment.gateway.service.X402GatewayOrderService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 默认 X402 facilitator 实现。 */
@Service
public class DefaultX402Facilitator implements X402Facilitator {

  private final X402GatewayOrderService gatewayOrderService;
  private final CryptoPaymentProperties properties;

  public DefaultX402Facilitator(
      X402GatewayOrderService gatewayOrderService,
      CryptoPaymentProperties properties) {
    this.gatewayOrderService = gatewayOrderService;
    this.properties = properties;
  }

  @Override
  public X402GatewayCreateOrderResponse createOrder(X402GatewayCreateOrderRequest request) {
    return gatewayOrderService.createOrder(request);
  }

  @Override
  public X402FacilitatorDescriptor describe() {
    boolean apiKeyRequired = StringUtils.hasText(properties.getGateway().getApiKey());
    return new X402FacilitatorDescriptor(
        properties.getGateway().getFacilitatorName(),
        "x402-1",
        apiKeyRequired,
        properties.getGateway().isPublicResourcesEnabled(),
        List.of("x402", "eip3009"),
        List.of("requirements", "support", "discovery", "payments", "eip3009-preview")
    );
  }
}
