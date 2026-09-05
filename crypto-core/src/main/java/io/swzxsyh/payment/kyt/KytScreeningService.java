package io.swzxsyh.payment.kyt;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.channel.address.DerivedAddressInventoryService;
import io.swzxsyh.payment.channel.address.DerivedAddressPoolService;
import io.swzxsyh.payment.alert.PaymentAlertService;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.ObjectProvider;

/** KYT 风控筛查服务。 */
@Slf4j
@Service
public class KytScreeningService {

  private final CryptoPaymentProperties properties;
  private final DerivedAddressInventoryService inventoryService;
  private final ObjectProvider<DerivedAddressPoolService> poolServiceProvider;
  private final PaymentAlertService alertService;
  private final List<KytProvider> providers;

  public KytScreeningService(
      CryptoPaymentProperties properties,
      DerivedAddressInventoryService inventoryService,
      ObjectProvider<DerivedAddressPoolService> poolServiceProvider,
      PaymentAlertService alertService,
      List<KytProvider> providers) {
    this.properties = properties;
    this.inventoryService = inventoryService;
    this.poolServiceProvider = poolServiceProvider;
    this.alertService = alertService;
    this.providers = new ArrayList<>(providers);
    this.providers.sort(Comparator.comparing(KytProvider::providerId));
  }

  public KytScreeningResult screen(KytScreeningRequest request) {
    if (!properties.getKyt().isEnabled()) {
      log.info("KYT disabled, returning passthrough result. cryptoOrderNo={}", request.cryptoOrderNo());
      return new KytScreeningResult(
          false,
          KytDecision.APPROVE,
          0,
          "disabled",
          List.of(),
          List.of("kyt disabled"),
          LocalDateTime.now()
      );
    }

    if (providers.isEmpty()) {
      log.warn("No KYT provider configured, using local fallback judgment. cryptoOrderNo={}",
          request.cryptoOrderNo());
      return LocalLocalKytResultFactory.fallback(properties, request);
    }

    List<KytProviderResult> results = new ArrayList<>();
    KytProviderResult selected = null;
    for (KytProvider provider : providers) {
      KytProviderResult result = provider.screen(request);
      results.add(result);
      if (selected == null || rank(result.decision()) > rank(selected.decision())
          || (result.decision() == selected.decision() && result.riskScore() > selected.riskScore())) {
        selected = result;
      }
    }

    int riskScore = selected == null ? 0 : selected.riskScore();
    KytDecision decision = selected == null ? KytDecision.APPROVE : selected.decision();
    List<String> reasons = selected == null ? List.of() : selected.reasons();

    log.info("KYT screening completed. cryptoOrderNo={}, decision={}, riskScore={}, providerCount={}",
        request.cryptoOrderNo(), decision, riskScore, results.size());
    if (decision == KytDecision.REJECT && StringUtils.hasText(request.payeeAddress())) {
      String poolKey = poolKey(request.chain(), request.token());
      if (inventoryService.findByPoolKeyAndAddress(poolKey, request.payeeAddress()).isPresent()) {
        DerivedAddressPoolService poolService = poolServiceProvider.getIfAvailable();
        if (poolService != null) {
          poolService.blockAddress(
              poolKey,
              request.payeeAddress(),
              String.join("; ", reasons),
              selected == null ? "local-fallback" : selected.providerId(),
              riskScore);
        } else {
          inventoryService.markRiskBlocked(
              poolKey,
              request.payeeAddress(),
              decision,
              selected == null ? "local-fallback" : selected.providerId(),
              riskScore,
              String.join("; ", reasons));
        }
        log.warn("Derived address blocked by KYT. cryptoOrderNo={}, poolKey={}, address={}, riskScore={}",
            request.cryptoOrderNo(), poolKey, request.payeeAddress(), riskScore);
        alertService.alertKytRejected(
            request.cryptoOrderNo(),
            request.chain(),
            request.token(),
            request.payeeAddress(),
            riskScore,
            selected == null ? "local-fallback" : selected.providerId());
      }
    }
    return new KytScreeningResult(
        true,
        decision,
        riskScore,
        selected == null ? "none" : selected.providerId(),
        results,
        reasons,
        LocalDateTime.now()
    );
  }

  private int rank(KytDecision decision) {
    return switch (decision) {
      case APPROVE -> 0;
      case REVIEW -> 1;
      case REJECT -> 2;
    };
  }

  private String poolKey(String chain, String token) {
    String safeChain = StringUtils.hasText(chain) ? chain.toUpperCase() : "UNKNOWN_CHAIN";
    String safeToken = StringUtils.hasText(token) ? token.toUpperCase() : "UNKNOWN_TOKEN";
    return RedisKeyNamespace.derivedAddressPool(properties, safeChain, safeToken);
  }

  private static final class LocalLocalKytResultFactory {

    private static KytScreeningResult fallback(
        CryptoPaymentProperties properties,
        KytScreeningRequest request) {
      List<String> reasons = new ArrayList<>();
      int riskScore = 0;
      KytDecision decision = KytDecision.APPROVE;

      if (StringUtils.hasText(request.payerAddress())
          && properties.getKyt().getDenyAddresses().stream().anyMatch(a -> a.equalsIgnoreCase(request.payerAddress()))) {
        reasons.add("payer address is denylisted");
        riskScore += 90;
        decision = KytDecision.REJECT;
      }
      if (StringUtils.hasText(request.payeeAddress())
          && properties.getKyt().getDenyAddresses().stream().anyMatch(a -> a.equalsIgnoreCase(request.payeeAddress()))) {
        reasons.add("payee address is denylisted");
        riskScore += 90;
        decision = KytDecision.REJECT;
      }
      if (StringUtils.hasText(request.chain())
          && properties.getKyt().getHighRiskChains().stream().anyMatch(a -> a.equalsIgnoreCase(request.chain()))) {
        reasons.add("chain is marked high risk");
        riskScore += 40;
      }
      if (StringUtils.hasText(request.token())
          && properties.getKyt().getHighRiskTokens().stream().anyMatch(a -> a.equalsIgnoreCase(request.token()))) {
        reasons.add("token is marked high risk");
        riskScore += 25;
      }
      if (request.walletAccountType() != null
          && request.walletAccountType().name().equalsIgnoreCase("SCA")) {
        reasons.add("smart account needs additional review");
        riskScore += 10;
      }
      if (riskScore >= properties.getKyt().getRejectThreshold()) {
        decision = KytDecision.REJECT;
      } else if (riskScore >= properties.getKyt().getReviewThreshold()) {
        decision = KytDecision.REVIEW;
      }

      return new KytScreeningResult(
          true,
          decision,
          riskScore,
          "local-fallback",
          List.of(new KytProviderResult(
              "local-fallback",
              decision,
              riskScore,
              reasons,
              reasons,
              LocalDateTime.now()
          )),
          reasons,
          LocalDateTime.now()
      );
    }
  }
}
