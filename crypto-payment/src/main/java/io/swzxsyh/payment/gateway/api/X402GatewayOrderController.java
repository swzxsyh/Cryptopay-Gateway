package io.swzxsyh.payment.gateway.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderRequest;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderResponse;
import io.swzxsyh.payment.gateway.x402.X402Facilitator;
import io.swzxsyh.payment.signature.SignatureScope;
import io.swzxsyh.payment.signature.SignatureVerified;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** X402 网关下单入口。 */
@RestController
@RequestMapping("/api/crypto/x402")
public class X402GatewayOrderController {

  private final X402Facilitator facilitator;

  public X402GatewayOrderController(
      X402Facilitator facilitator) {
    this.facilitator = facilitator;
  }

  /** 创建网关订单。 */
  @SignatureVerified(scope = SignatureScope.GATEWAY_CREATE)
  @PostMapping("/orders")
  public ApiResponse<X402GatewayCreateOrderResponse> createOrder(@RequestBody X402GatewayCreateOrderRequest request) {
    return ApiResponse.ok(facilitator.createOrder(request));
  }
}
