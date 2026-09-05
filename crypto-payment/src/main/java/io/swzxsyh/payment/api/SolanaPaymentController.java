package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.solana.SolanaFeePayerSignRequest;
import io.swzxsyh.payment.solana.SolanaFeePayerSignResponse;
import io.swzxsyh.payment.solana.SolanaFeeEstimateRequest;
import io.swzxsyh.payment.solana.SolanaFeeEstimateResponse;
import io.swzxsyh.payment.solana.SolanaPaymentIntent;
import io.swzxsyh.payment.solana.SolanaPaymentService;
import io.swzxsyh.payment.solana.SolanaRawTransactionRequest;
import io.swzxsyh.payment.solana.SolanaRawTransactionResponse;
import io.swzxsyh.payment.sponsor.GasSponsorGuardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

/** Solana 支付协议接口，提供钱包支付意图、费用预估和交易广播。 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/solana")
public class SolanaPaymentController {

  private final SolanaPaymentService solanaPaymentService;
  private final PaymentOrderService paymentOrderService;
  private final GasSponsorGuardService gasSponsorGuardService;

  public SolanaPaymentController(
      SolanaPaymentService solanaPaymentService,
      PaymentOrderService paymentOrderService,
      GasSponsorGuardService gasSponsorGuardService) {
    this.solanaPaymentService = solanaPaymentService;
    this.paymentOrderService = paymentOrderService;
    this.gasSponsorGuardService = gasSponsorGuardService;
  }

  /** 获取 Solana 钱包支付意图，前端据此构造钱包交易。 */
  @GetMapping("/orders/{cryptoOrderNo}/intent")
  public ApiResponse<SolanaPaymentIntent> intent(@PathVariable String cryptoOrderNo) {
    return ApiResponse.ok(solanaPaymentService.buildPaymentIntent(cryptoOrderNo));
  }

  /** 使用前端构造出的 message 预估 Solana 交易手续费。 */
  @PostMapping("/orders/{cryptoOrderNo}/fee-estimate")
  public ApiResponse<SolanaFeeEstimateResponse> estimateFee(
      @PathVariable String cryptoOrderNo,
      @RequestBody SolanaFeeEstimateRequest request) {
    PaymentOrder order = paymentOrderService.getOrder(cryptoOrderNo);
    String chain = StringUtils.hasText(request.chain()) ? request.chain() : order.getChain();
    return ApiResponse.ok(
        solanaPaymentService.estimateFee(
            new SolanaFeeEstimateRequest(chain, request.messageBase64(), request.signerCount())));
  }

  /** 平台 fee payer 对 Solana transaction message 做第一段签名，用户钱包仍需做第二段签名。 */
  @PostMapping("/orders/{cryptoOrderNo}/fee-payer-sign")
  public ApiResponse<SolanaFeePayerSignResponse> signFeePayerMessage(
      @PathVariable String cryptoOrderNo,
      @RequestBody SolanaFeePayerSignRequest request) {
    PaymentOrder order = paymentOrderService.getOrder(cryptoOrderNo);
    String chain = StringUtils.hasText(request.chain()) ? request.chain() : order.getChain();
    return ApiResponse.ok(
        gasSponsorGuardService.execute(
            order,
            "SOLANA_FEE_PAYER",
            "FEE_PAYER_SIGN",
            order.getWalletAddress(),
            () -> solanaPaymentService.signFeePayerMessage(
                order,
                new SolanaFeePayerSignRequest(chain, request.messageBase64()))));
  }

  /** 广播已由钱包完成签名的 Solana raw transaction，并把 signature 回写为订单 txHash。 */
  @PostMapping("/orders/{cryptoOrderNo}/send-raw-transaction")
  public ApiResponse<SolanaRawTransactionResponse> sendRawTransaction(
      @PathVariable String cryptoOrderNo,
      @RequestBody SolanaRawTransactionRequest request) {
    PaymentOrder order = paymentOrderService.getOrder(cryptoOrderNo);
    String chain = StringUtils.hasText(request.chain()) ? request.chain() : order.getChain();
    SolanaRawTransactionResponse response =
        solanaPaymentService.sendRawTransaction(
            new SolanaRawTransactionRequest(chain, request.signedTransactionBase64()));
    paymentOrderService.reportPaymentTxHash(cryptoOrderNo, response.signature());
    return ApiResponse.ok(response);
  }
}
