package io.swzxsyh.payment.kyt;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 本地规则 KYT 提供方。 */
@Component
public class LocalRulesKytProvider implements KytProvider {

  private final CryptoPaymentProperties properties;

  public LocalRulesKytProvider(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public String providerId() {
    return "local-rules";
  }

  @Override
  public KytProviderResult screen(KytScreeningRequest request) {
    List<String> reasons = new ArrayList<>();
    List<String> signals = new ArrayList<>();
    int score = 0;

    if (StringUtils.hasText(request.payerAddress())
        && properties.getKyt().getAllowAddresses().stream().anyMatch(a -> a.equalsIgnoreCase(request.payerAddress()))) {
      reasons.add("payer address allowlisted");
      signals.add("allowlist-payer");
      score = Math.max(0, score - 20);
    }
    if (StringUtils.hasText(request.payeeAddress())
        && properties.getKyt().getAllowAddresses().stream().anyMatch(a -> a.equalsIgnoreCase(request.payeeAddress()))) {
      reasons.add("payee address allowlisted");
      signals.add("allowlist-payee");
      score = Math.max(0, score - 20);
    }
    if (request.amount() != null && request.amount().compareTo(new java.math.BigDecimal("10000")) >= 0) {
      reasons.add("amount above high-value threshold");
      signals.add("high-value-amount");
      score += 15;
    }
    if (request.walletAccountType() != null && request.walletAccountType().name().equalsIgnoreCase("SCA")) {
      reasons.add("smart account path requires enhanced review");
      signals.add("sca-wallet");
      score += 10;
    }
    if (StringUtils.hasText(request.cryptoOrderNo())) {
      signals.add("order-linked");
    }

    KytDecision decision;
    if (properties.getKyt().getDenyAddresses().stream()
        .anyMatch(a -> a.equalsIgnoreCase(request.payerAddress()))
        || properties.getKyt().getDenyAddresses().stream()
        .anyMatch(a -> a.equalsIgnoreCase(request.payeeAddress()))) {
      decision = KytDecision.REJECT;
      score = Math.max(score, properties.getKyt().getRejectThreshold());
      reasons.add("matched denylist");
    } else if (score >= properties.getKyt().getRejectThreshold()) {
      decision = KytDecision.REJECT;
    } else if (score >= properties.getKyt().getReviewThreshold()) {
      decision = KytDecision.REVIEW;
    } else {
      decision = KytDecision.APPROVE;
    }

    return new KytProviderResult(
        providerId(),
        decision,
        score,
        reasons,
        signals,
        LocalDateTime.now()
    );
  }
}
