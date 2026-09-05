package io.swzxsyh.payment.precheck;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.kyt.KytDecision;
import io.swzxsyh.payment.kyt.KytScreeningRequest;
import io.swzxsyh.payment.kyt.KytScreeningResult;
import io.swzxsyh.payment.kyt.KytScreeningService;
import io.swzxsyh.payment.routing.TokenCapabilityProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 托管钱包支付前置校验服务。
 *
 * <p>这里做的是“支付信息下发前”的弱风控：
 * 先确认钱包地址、链、币种和基础 KYT 结果，再决定是否继续返回支付参数。
 */
@Slf4j
@Service
public class HostedWalletPrecheckService {

  private final CryptoPaymentProperties properties;
  private final KytScreeningService kytScreeningService;

  public HostedWalletPrecheckService(
      CryptoPaymentProperties properties,
      KytScreeningService kytScreeningService) {
    this.properties = properties;
    this.kytScreeningService = kytScreeningService;
  }

  /**
   * 对托管钱包支付进行预检。
   *
   * <p>这里的定位是“支付参数下发前”的前置筛查，不是链上拦截。
   * <ul>
   *   <li>KYT 未开启时，{@link KytScreeningService} 会直接返回 APPROVE，流程正常放行。</li>
   *   <li>KYT 命中 REJECT 时，直接阻断托管钱包支付。</li>
   *   <li>如果开启 strictMode，则 REVIEW 也会被视为需要拦截。</li>
   * </ul>
   */
  public KytScreeningResult precheck(
      PaymentOrder order,
      PaymentSelection selection,
      TokenCapabilityProfile tokenProfile,
      String payeeAddress) {
    if (order == null) {
      throw new IllegalArgumentException("order is required");
    }
    if (selection == null) {
      throw new IllegalArgumentException("selection is required");
    }
    if (tokenProfile == null) {
      throw new IllegalArgumentException("tokenProfile is required");
    }
    if (!StringUtils.hasText(selection.walletAddress())) {
      throw new IllegalArgumentException("walletAddress is required for hosted wallet payment");
    }

    KytScreeningResult result = kytScreeningService.screen(new KytScreeningRequest(
        tokenProfile.chain(),
        tokenProfile.token(),
        tokenProfile.tokenAddress(),
        null,
        order.getCryptoOrderNo(),
        selection.walletAddress(),
        payeeAddress,
        selection.walletAccountType(),
        order.getAmount()
    ));

    if (result.decision() == KytDecision.REJECT
        || (properties.getKyt().isStrictMode() && result.decision() == KytDecision.REVIEW)) {
      log.warn(
          "Hosted wallet precheck rejected. cryptoOrderNo={}, chain={}, token={}, walletAddress={}, decision={}, riskScore={}, reasons={}",
          order.getCryptoOrderNo(),
          tokenProfile.chain(),
          tokenProfile.token(),
          selection.walletAddress(),
          result.decision(),
          result.riskScore(),
          result.reasons());
      throw new IllegalStateException("hosted wallet precheck rejected: " + String.join("; ", result.reasons()));
    }

    if (result.decision() == KytDecision.REVIEW) {
      log.warn(
          "Hosted wallet precheck requires review. cryptoOrderNo={}, chain={}, token={}, walletAddress={}, riskScore={}, reasons={}",
          order.getCryptoOrderNo(),
          tokenProfile.chain(),
          tokenProfile.token(),
          selection.walletAddress(),
          result.riskScore(),
          result.reasons());
    } else {
      // KYT 关闭时，这里就是 APPROVE；或者命中低风险规则时，也允许继续走后续支付路由。
      log.info(
          "Hosted wallet precheck approved. cryptoOrderNo={}, chain={}, token={}, walletAddress={}, riskScore={}",
          order.getCryptoOrderNo(),
          tokenProfile.chain(),
          tokenProfile.token(),
          selection.walletAddress(),
          result.riskScore());
    }

    return result;
  }
}
