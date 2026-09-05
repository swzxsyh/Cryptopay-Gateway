package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.sponsor.Eip3009AuthorizationSubmission;
import io.swzxsyh.payment.sponsor.GasSponsorExecutionCommand;
import io.swzxsyh.payment.sponsor.GasSponsorExecutionResult;
import io.swzxsyh.payment.sponsor.GasSponsorGuardService;
import io.swzxsyh.payment.sponsor.GasSponsorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** EVM Gas 代付接口，目前支持 EIP-3009 transferWithAuthorization relayer 提交。 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/evm/sponsor")
public class EvmGasSponsorController {

  private final GasSponsorRegistry gasSponsorRegistry;
  private final GasSponsorGuardService gasSponsorGuardService;
  private final PaymentOrderService paymentOrderService;

  public EvmGasSponsorController(
      GasSponsorRegistry gasSponsorRegistry,
      GasSponsorGuardService gasSponsorGuardService,
      PaymentOrderService paymentOrderService) {
    this.gasSponsorRegistry = gasSponsorRegistry;
    this.gasSponsorGuardService = gasSponsorGuardService;
    this.paymentOrderService = paymentOrderService;
  }

  /** 提交用户 EIP-3009 授权签名，由平台 relayer 支付 gas 调用 token 合约。 */
  @PostMapping("/orders/{cryptoOrderNo}/eip3009/submit")
  public ApiResponse<GasSponsorExecutionResult> submitEip3009(
      @PathVariable String cryptoOrderNo,
      @RequestBody Eip3009AuthorizationSubmission authorization) {
    PaymentOrder order = paymentOrderService.getOrder(cryptoOrderNo);
    GasSponsorExecutionResult result =
        gasSponsorGuardService.execute(
            order,
            "EIP3009",
            "TRANSFER_WITH_AUTHORIZATION",
            authorization == null ? null : authorization.from(),
            () -> gasSponsorRegistry.execute(
                "EIP3009",
                new GasSponsorExecutionCommand(order, authorization)));
    if (result.submitted()) {
      paymentOrderService.reportPaymentTxHash(cryptoOrderNo, result.txHash());
    }
    log.info("EIP-3009 sponsor submitted. cryptoOrderNo={}, txHash={}", cryptoOrderNo, result.txHash());
    return ApiResponse.ok(result);
  }
}
