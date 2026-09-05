package io.swzxsyh.payment.gateway.stage;

import io.swzxsyh.payment.api.dto.CreateCryptoOrderResponse;
import io.swzxsyh.payment.api.dto.MerchantPaymentOrderView;
import io.swzxsyh.payment.gateway.model.GatewayContext;
import io.swzxsyh.payment.service.PaymentOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 网关下单阶段。 */
@Slf4j
@Component
@Order(100)
public class CreateOrderStage implements GatewayStage {

  private final PaymentOrderService paymentOrderService;

  public CreateOrderStage(PaymentOrderService paymentOrderService) {
    this.paymentOrderService = paymentOrderService;
  }

  @Override
  public void apply(GatewayContext context) {
    var request = context.getRequest();
    var order = paymentOrderService.createOrder(
        request.merchantId(),
        request.merchantOrderNo(),
        request.amount(),
        request.currency(),
        request.notifyUrl(),
        request.returnUrl(),
        request.idempotencyKey()
    );
    String cashierToken = paymentOrderService.buildCashierToken(order);
    String cashierUrl = paymentOrderService.buildCashierUrl(cashierToken);
    context.setOrderResponse(new CreateCryptoOrderResponse(
        order.getCryptoOrderNo(),
        cashierToken,
        cashierUrl,
        MerchantPaymentOrderView.from(order)
    ));
    log.info("Created downstream crypto order after gateway pass. merchantId={}, cryptoOrderNo={}, cashierUrl={}",
        request.merchantId(), order.getCryptoOrderNo(), cashierUrl);
  }
}
