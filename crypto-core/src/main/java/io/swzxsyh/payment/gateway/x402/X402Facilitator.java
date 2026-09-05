package io.swzxsyh.payment.gateway.x402;

import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderRequest;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderResponse;

/** X402 facilitator 抽象。 */
public interface X402Facilitator {

  X402GatewayCreateOrderResponse createOrder(X402GatewayCreateOrderRequest request);

  X402FacilitatorDescriptor describe();
}
