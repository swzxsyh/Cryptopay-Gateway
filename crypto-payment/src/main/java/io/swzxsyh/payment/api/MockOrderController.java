package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.api.dto.CreateCryptoOrderRequest;
import io.swzxsyh.payment.api.dto.CreateCryptoOrderResponse;
import io.swzxsyh.payment.api.dto.MerchantPaymentOrderView;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.subscription.SubscriptionOrderService;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderResponse;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 本地测试用的模拟下单测试接口。 */
@Profile("dev")
@RestController
@RequestMapping("/mock")
public class MockOrderController {

  private static final String DEFAULT_MOCK_MERCHANT_ID = "MOCK-MERCHANT-001";

  private final PaymentOrderService paymentOrderService;
  private final SubscriptionOrderService subscriptionOrderService;

  public MockOrderController(
      PaymentOrderService paymentOrderService, SubscriptionOrderService subscriptionOrderService) {
    this.paymentOrderService = paymentOrderService;
    this.subscriptionOrderService = subscriptionOrderService;
  }

  /** 返回 mock 测试接口总览信息。 */
  @GetMapping
  public ApiResponse<List<String>> overview() {
    return ApiResponse.ok(List.of("POST /mock/api/orders", "POST /mock/api/subscriptions/orders"));
  }

  /** 调用普通下单接口。 */
  @PostMapping("/api/orders")
  public ApiResponse<CreateCryptoOrderResponse> createOrder(
      @RequestBody CreateCryptoOrderRequest request) {
    PaymentOrder order =
        paymentOrderService.createOrder(
            resolveMerchantId(request.merchantId()),
            request.merchantOrderNo(),
            request.amount(),
            request.currency(),
            request.notifyUrl(),
            request.returnUrl(),
            request.idempotencyKey());
    String cashierToken = paymentOrderService.buildCashierToken(order);
    String cashierUrl = paymentOrderService.buildCashierUrl(cashierToken);
    return ApiResponse.ok(
        new CreateCryptoOrderResponse(
            order.getCryptoOrderNo(), cashierToken, cashierUrl, MerchantPaymentOrderView.from(order)));
  }

  /** 调用订阅下单接口。 */
  @PostMapping("/api/subscriptions/orders")
  public ApiResponse<CreateSubscriptionOrderResponse> createSubscriptionOrder(
      @RequestBody CreateSubscriptionOrderRequest request) {
    CreateSubscriptionOrderRequest mockRequest =
        new CreateSubscriptionOrderRequest(
            request.merchantOrderNo(),
            resolveMerchantId(request.merchantId()),
            request.amountPerCycle(),
            request.currency(),
            request.chain(),
            request.token(),
            request.tokenAddress(),
            request.payerAddress(),
            request.recipientAddress(),
            request.billingMode(),
            request.cycleSeconds(),
            request.notifyUrl(),
            request.returnUrl(),
            request.idempotencyKey());
    return ApiResponse.ok(subscriptionOrderService.createOrder(mockRequest));
  }

  private String resolveMerchantId(String merchantId) {
    return StringUtils.hasText(merchantId)
        ? merchantId.trim()
        : DEFAULT_MOCK_MERCHANT_ID;
  }
}
