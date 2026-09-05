package io.swzxsyh.payment.subscription;

import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.kyt.KytDecision;
import io.swzxsyh.payment.kyt.KytScreeningRequest;
import io.swzxsyh.payment.kyt.KytScreeningResult;
import io.swzxsyh.payment.kyt.KytScreeningService;
import io.swzxsyh.payment.routing.WalletAccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 订阅支付 KYT 守卫，统一处理订阅前置筛查和账单到账后的后置筛查。 */
@Slf4j
@Service
public class SubscriptionKytGuard {

  private final CryptoPaymentProperties properties;
  private final KytScreeningService kytScreeningService;
  private final PaymentAuditService auditService;

  public SubscriptionKytGuard(
      CryptoPaymentProperties properties,
      KytScreeningService kytScreeningService,
      PaymentAuditService auditService) {
    this.properties = properties;
    this.kytScreeningService = kytScreeningService;
    this.auditService = auditService;
  }

  /**
   * 订阅创建阶段的前置 KYT。
   *
   * <p>KYT 未开启时直接放行；命中拒绝，或严格模式下命中复核，会阻断订阅创建，
   * 避免提前下发合约调用参数。
   */
  public KytScreeningResult precheckSetup(SubscriptionOrder order) {
    KytScreeningResult result = screen(order, null, order.getAmountPerCycle(), "SUBSCRIPTION_KYT_PRECHECK");
    if (precheckBlocked(result)) {
      auditService.record(
          "SUBSCRIPTION_KYT_PRECHECK_BLOCKED",
          "SUBSCRIPTION_ORDER",
          order.getSubscriptionOrderNo(),
          result.decision().name(),
          result);
      throw new IllegalStateException("Subscription KYT precheck rejected: " + reason(result));
    }
    log.info("订阅前置 KYT 通过。subscriptionOrderNo={}, enabled={}, decision={}, riskScore={}",
        order.getSubscriptionOrderNo(), result.enabled(), result.decision(), result.riskScore());
    return result;
  }

  /**
   * 订阅账单到账后的后置 KYT。
   *
   * <p>账单链上成功后必须先经过这里，只有 APPROVE 才允许进入 PAID、商户余额入账和商户回调。
   */
  public KytScreeningResult postcheckBilling(
      SubscriptionOrder order, SubscriptionBillingRecord bill, String txHash, BigDecimal realAmount) {
    KytScreeningResult result =
        screen(order, bill == null ? null : bill.getBillingSequence(), realAmount, "SUBSCRIPTION_KYT_POSTCHECK");
    log.info("订阅后置 KYT 完成。subscriptionOrderNo={}, billingSequence={}, txHash={}, enabled={}, decision={}, riskScore={}",
        order.getSubscriptionOrderNo(),
        bill == null ? null : bill.getBillingSequence(),
        txHash,
        result.enabled(),
        result.decision(),
        result.riskScore());
    return result;
  }

  /** 判断后置 KYT 是否需要人工处理。 */
  public boolean requiresManualHandling(KytScreeningResult result) {
    return result != null
        && result.enabled()
        && (result.decision() == KytDecision.REJECT || result.decision() == KytDecision.REVIEW);
  }

  /** 构造可落库的 KYT 原因摘要。 */
  public String reason(KytScreeningResult result) {
    if (result == null) {
      return "KYT result missing";
    }
    String reasons = result.reasons() == null || result.reasons().isEmpty()
        ? "no detail reasons"
        : String.join("; ", result.reasons());
    return "decision=" + result.decision()
        + ", provider=" + result.selectedProvider()
        + ", riskScore=" + result.riskScore()
        + ", reasons=" + reasons;
  }

  private KytScreeningResult screen(
      SubscriptionOrder order, Integer billingSequence, BigDecimal amount, String eventType) {
    try {
      return kytScreeningService.screen(new KytScreeningRequest(
          order.getChain(),
          order.getToken(),
          order.getTokenAddress(),
          order.getMerchantId(),
          order.getSubscriptionOrderNo()
              + (billingSequence == null ? "" : ":" + billingSequence),
          order.getPayerAddress(),
          order.getRecipientAddress(),
          WalletAccountType.UNKNOWN,
          amount));
    } catch (Exception ex) {
      if (!properties.getKyt().isEnabled() || !properties.getKyt().isStrictMode()) {
        log.warn("订阅 KYT 执行异常，当前为非严格模式，继续放行。eventType={}, subscriptionOrderNo={}, error={}",
            eventType, order.getSubscriptionOrderNo(), ex.getMessage(), ex);
        return new KytScreeningResult(
            properties.getKyt().isEnabled(),
            KytDecision.APPROVE,
            0,
            "kyt-error-passthrough",
            List.of(),
            List.of("kyt provider error: " + ex.getMessage()),
            LocalDateTime.now());
      }
      log.error("订阅 KYT 执行异常，严格模式下进入人工复核。eventType={}, subscriptionOrderNo={}, error={}",
          eventType, order.getSubscriptionOrderNo(), ex.getMessage(), ex);
      return new KytScreeningResult(
          true,
          KytDecision.REVIEW,
          properties.getKyt().getReviewThreshold(),
          "kyt-error-strict",
          List.of(),
          List.of("kyt provider error: " + ex.getMessage()),
          LocalDateTime.now());
    }
  }

  private boolean precheckBlocked(KytScreeningResult result) {
    return result != null
        && result.enabled()
        && (result.decision() == KytDecision.REJECT
            || (properties.getKyt().isStrictMode() && result.decision() == KytDecision.REVIEW));
  }
}
