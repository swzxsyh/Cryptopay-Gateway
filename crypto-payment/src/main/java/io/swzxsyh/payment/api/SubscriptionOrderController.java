package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.signature.SignatureScope;
import io.swzxsyh.payment.signature.SignatureVerified;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.SubscriptionOrderService;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderResponse;
import io.swzxsyh.payment.subscription.dto.SubmitSubscriptionSetupTxRequest;
import io.swzxsyh.payment.subscription.dto.SubscriptionActionResponse;
import io.swzxsyh.payment.subscription.dto.SubscriptionCashierBootstrap;
import io.swzxsyh.payment.subscription.dto.SubscriptionOrderView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户订阅侧接口。
 *
 * <p>提供订阅下单和查单能力，供主系统或商户侧接入。
 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/subscriptions")
public class SubscriptionOrderController {

  private final SubscriptionOrderService subscriptionOrderService;

  public SubscriptionOrderController(SubscriptionOrderService subscriptionOrderService) {
    this.subscriptionOrderService = subscriptionOrderService;
  }

  /** 创建订阅订单。 */
  @SignatureVerified(scope = SignatureScope.SUBSCRIPTION_CREATE)
  @PostMapping("/orders")
  public ApiResponse<CreateSubscriptionOrderResponse> createOrder(@RequestBody CreateSubscriptionOrderRequest request) {
    return ApiResponse.ok(subscriptionOrderService.createOrder(request));
  }

  /** 查询订阅订单。 */
  @SignatureVerified(scope = SignatureScope.SUBSCRIPTION_QUERY)
  @GetMapping("/orders/{subscriptionOrderNo}")
  public ApiResponse<SubscriptionOrderView> getOrder(@PathVariable String subscriptionOrderNo) {
    SubscriptionOrder order = subscriptionOrderService.getOrder(subscriptionOrderNo);
    return ApiResponse.ok(SubscriptionOrderView.from(order));
  }

  /** 查询订阅收银台启动数据，公开入口只接收收银台 token，不接收明文订阅单号。 */
  @GetMapping("/cashier/{subscriptionToken}")
  public ApiResponse<SubscriptionCashierBootstrap> cashier(
      @PathVariable String subscriptionToken) {
    return ApiResponse.ok(subscriptionOrderService.getCashierBootstrapByToken(subscriptionToken));
  }

  /** 前端钱包发起订阅初始化后，上报链上交易哈希。 */
  @PostMapping("/cashier/{subscriptionToken}/setup-tx")
  public ApiResponse<SubscriptionActionResponse> submitCashierSetupTx(
      @PathVariable String subscriptionToken,
      @RequestBody SubmitSubscriptionSetupTxRequest request) {
    return ApiResponse.ok(subscriptionOrderService.submitSetupTxByToken(subscriptionToken, request.txHash()));
  }

  /** 商户或主系统上报订阅初始化交易哈希。 */
  @SignatureVerified(scope = SignatureScope.SUBSCRIPTION_SETUP)
  @PostMapping("/orders/{subscriptionOrderNo}/setup-tx")
  public ApiResponse<SubscriptionActionResponse> submitSetupTx(
      @PathVariable String subscriptionOrderNo,
      @RequestBody SubmitSubscriptionSetupTxRequest request) {
    return ApiResponse.ok(subscriptionOrderService.submitSetupTx(subscriptionOrderNo, request.txHash()));
  }

  /** 暂停订阅。 */
  @SignatureVerified(scope = SignatureScope.SUBSCRIPTION_MANAGE)
  @PostMapping("/orders/{subscriptionOrderNo}/pause")
  public ApiResponse<SubscriptionActionResponse> pause(@PathVariable String subscriptionOrderNo) {
    return ApiResponse.ok(subscriptionOrderService.pause(subscriptionOrderNo));
  }

  /** 恢复订阅。 */
  @SignatureVerified(scope = SignatureScope.SUBSCRIPTION_MANAGE)
  @PostMapping("/orders/{subscriptionOrderNo}/resume")
  public ApiResponse<SubscriptionActionResponse> resume(@PathVariable String subscriptionOrderNo) {
    return ApiResponse.ok(subscriptionOrderService.resume(subscriptionOrderNo));
  }

  /** 取消订阅。 */
  @SignatureVerified(scope = SignatureScope.SUBSCRIPTION_MANAGE)
  @PostMapping("/orders/{subscriptionOrderNo}/cancel")
  public ApiResponse<SubscriptionActionResponse> cancel(@PathVariable String subscriptionOrderNo) {
    return ApiResponse.ok(subscriptionOrderService.cancel(subscriptionOrderNo));
  }
}
