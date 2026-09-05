package io.swzxsyh.payment.gateway.service;

import io.swzxsyh.payment.gateway.dto.GatewayFeeReceiptView;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderRequest;
import io.swzxsyh.payment.gateway.dto.X402GatewayCreateOrderResponse;
import io.swzxsyh.payment.gateway.model.GatewayContext;
import io.swzxsyh.payment.gateway.model.GatewayFeeReceipt;
import io.swzxsyh.payment.gateway.stage.GatewayStage;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.idempotency.DbIdempotencyService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

/** X402 网关订单编排服务。 */
@Slf4j
@Service
public class X402GatewayOrderService {

  private final List<GatewayStage> stages;
  private final DbIdempotencyService idempotencyService;
  private final PaymentAuditService auditService;

  public X402GatewayOrderService(
      List<GatewayStage> stages,
      DbIdempotencyService idempotencyService,
      PaymentAuditService auditService) {
    this.stages = stages;
    this.idempotencyService = idempotencyService;
    this.auditService = auditService;
    AnnotationAwareOrderComparator.sort(this.stages);
  }

  public X402GatewayCreateOrderResponse createOrder(X402GatewayCreateOrderRequest request) {
    return idempotencyService.executeOnceRequired(
        "gateway-order:" + request.merchantId(),
        request.idempotencyKey(),
        X402GatewayCreateOrderResponse.class,
        () -> createOrderInternal(request));
  }

  private X402GatewayCreateOrderResponse createOrderInternal(X402GatewayCreateOrderRequest request) {
    GatewayContext context = new GatewayContext(request);
    for (GatewayStage stage : stages) {
      stage.apply(context);
    }

    GatewayFeeReceipt feeReceipt = context.getFeeReceipt();
    log.info("Gateway order finished. merchantId={}, merchantOrderNo={}, feeReceipt={}",
        request.merchantId(), request.merchantOrderNo(), feeReceipt == null ? "none" : feeReceipt.receiptNo());

    X402GatewayCreateOrderResponse response = new X402GatewayCreateOrderResponse(
        feeReceipt == null ? null : new GatewayFeeReceiptView(
            feeReceipt.receiptNo(),
            feeReceipt.merchantId(),
            feeReceipt.feeToken(),
            feeReceipt.feeAmount(),
            feeReceipt.remainingBalance()
        ),
        context.getOrderResponse()
    );
    auditService.record("GATEWAY_ORDER_CREATED", "GATEWAY_ORDER", request.merchantOrderNo(), "CREATED", response);
    return response;
  }
}
