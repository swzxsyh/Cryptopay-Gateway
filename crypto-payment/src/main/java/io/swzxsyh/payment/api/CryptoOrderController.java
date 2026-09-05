package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.api.dto.CreateCryptoOrderRequest;
import io.swzxsyh.payment.api.dto.CreateCryptoOrderResponse;
import io.swzxsyh.payment.api.dto.CashierPaymentOrderView;
import io.swzxsyh.payment.api.dto.MerchantPaymentOrderView;
import io.swzxsyh.payment.api.dto.ReportPaymentTxHashRequest;
import io.swzxsyh.payment.api.dto.SelectPaymentMethodRequest;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.pending.PendingChainTransactionService;
import io.swzxsyh.payment.api.dto.PendingChainTransactionView;
import io.swzxsyh.payment.routing.WalletAccountType;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.signature.SignatureScope;
import io.swzxsyh.payment.signature.SignatureVerified;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户支付侧订单接口。
 *
 * <p>负责下单、查单、选择支付方式和上报 txHash，是主系统对接收银台的核心入口。
 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/orders")
public class CryptoOrderController {

  private final PaymentOrderService paymentOrderService;
  private final PendingChainTransactionService pendingTransactionService;

  public CryptoOrderController(
      PaymentOrderService paymentOrderService,
      PendingChainTransactionService pendingTransactionService) {
    this.paymentOrderService = paymentOrderService;
    this.pendingTransactionService = pendingTransactionService;
  }

  /** 创建普通支付订单。 */
  @SignatureVerified(scope = SignatureScope.ORDER_CREATE)
  @PostMapping
  public ApiResponse<CreateCryptoOrderResponse> createOrder(@RequestBody CreateCryptoOrderRequest request) {
    PaymentOrder order = paymentOrderService.createOrder(
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
    log.info("Created cashier URL. cryptoOrderNo={}, cashierUrl={}", order.getCryptoOrderNo(), cashierUrl);
    return ApiResponse.ok(new CreateCryptoOrderResponse(
        order.getCryptoOrderNo(), cashierToken, cashierUrl, MerchantPaymentOrderView.from(order)));
  }

  /** 查询普通支付订单详情。 */
  @SignatureVerified(scope = SignatureScope.ORDER_QUERY)
  @GetMapping("/{cryptoOrderNo}")
  public ApiResponse<MerchantPaymentOrderView> getOrder(@PathVariable String cryptoOrderNo) {
    return ApiResponse.ok(MerchantPaymentOrderView.from(paymentOrderService.getOrder(cryptoOrderNo)));
  }

  /** 根据链上交易哈希主动查询订单。 */
  // @SignatureVerified(scope = SignatureScope.ORDER_QUERY)
  // @GetMapping("/tx/{paymentTxHash}")
  // public ApiResponse<MerchantPaymentOrderView> getOrderByPaymentTxHash(@PathVariable String paymentTxHash) {
  //   return ApiResponse.ok(view(paymentOrderService.getOrderByPaymentTxHash(paymentTxHash)));
  // }

  /** 为订单选择最终支付方式并生成支付详情。 */
  @PostMapping("/{cryptoOrderNo}/select-method")
  public ApiResponse<CashierPaymentOrderView> selectPaymentMethod(
      @PathVariable String cryptoOrderNo,
      @RequestBody SelectPaymentMethodRequest request) {
    PaymentSelection selection = new PaymentSelection(
        request.paymentMethod(),
        request.chain(),
        request.token(),
        request.tokenAddress(),
        request.walletAddress(),
        request.walletAccountType() == null ? WalletAccountType.UNKNOWN : request.walletAccountType()
    );
    return ApiResponse.ok(view(paymentOrderService.selectPaymentMethod(cryptoOrderNo, selection)));
  }

  /** 前端钱包广播交易后上报 txHash，后端后续仍以链上确认结果为准。 */
  @PostMapping("/{cryptoOrderNo}/tx-hash")
  public ApiResponse<CashierPaymentOrderView> reportPaymentTxHash(
      @PathVariable String cryptoOrderNo,
      @RequestBody ReportPaymentTxHashRequest request) {
    return ApiResponse.ok(
        view(paymentOrderService.reportPaymentTxHash(cryptoOrderNo, request.txHash())));
  }

  private CashierPaymentOrderView view(PaymentOrder order) {
    PendingChainTransactionView pending =
        pendingTransactionService
            .findLatestByOrderNo(order.getCryptoOrderNo())
            .map(PendingChainTransactionView::from)
            .orElse(null);
    return CashierPaymentOrderView.from(order, pending);
  }
}
