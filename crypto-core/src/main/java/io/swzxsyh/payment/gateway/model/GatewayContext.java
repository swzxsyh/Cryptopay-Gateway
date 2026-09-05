package io.swzxsyh.payment.gateway.model;

import io.swzxsyh.payment.api.dto.CreateCryptoOrderResponse;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderRequest;

/** X402 网关订单编排上下文。 */
public class GatewayContext {

  private final X402GatewayCreateOrderRequest request;
  private GatewayFeeReceipt feeReceipt;
  private CreateCryptoOrderResponse orderResponse;

  public GatewayContext(X402GatewayCreateOrderRequest request) {
    this.request = request;
  }

  public X402GatewayCreateOrderRequest getRequest() {
    return request;
  }

  public GatewayFeeReceipt getFeeReceipt() {
    return feeReceipt;
  }

  public void setFeeReceipt(GatewayFeeReceipt feeReceipt) {
    this.feeReceipt = feeReceipt;
  }

  public CreateCryptoOrderResponse getOrderResponse() {
    return orderResponse;
  }

  public void setOrderResponse(CreateCryptoOrderResponse orderResponse) {
    this.orderResponse = orderResponse;
  }
}
