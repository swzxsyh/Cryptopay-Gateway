package io.swzxsyh.payment.gas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GasPolicyPlanner {

  private final List<GasPolicyProvider> providers;

  public GasPolicyPlanner(List<GasPolicyProvider> providers) {
    this.providers = new ArrayList<>(providers);
    AnnotationAwareOrderComparator.sort(this.providers);
  }

  public GasPolicyResult plan(GasPolicyRequest request) {
    if (providers.isEmpty()) {
      throw new IllegalStateException("No gas policy provider configured");
    }

    GasPolicyResult best = null;
    for (GasPolicyProvider provider : providers) {
      GasPolicyResult result = provider.plan(request);
      if (best == null || compare(result, best) > 0) {
        best = result;
      }
    }

    log.info("Planned gas policy. chain={}, token={}, payerMode={}, provider={}",
        request.chain(), request.token(), best.payerMode(), best.providerId());
    return best;
  }

  private int compare(GasPolicyResult left, GasPolicyResult right) {
    return Comparator
        .comparing((GasPolicyResult r) -> priority(r.payerMode()))
        .thenComparing(GasPolicyResult::customerBalanceSufficient)
        .thenComparing(GasPolicyResult::platformBalanceSufficient)
        .compare(left, right);
  }

  private int priority(GasPayerMode mode) {
    return switch (mode) {
      case PLATFORM_SPONSORED -> 5;
      case CUSTOMER_PAYS -> 4;
      case FREE_TRANSFER -> 3;
      case CUSTOMER_TOP_UP_REQUIRED -> 2;
      case ALTERNATE_ROUTE_REQUIRED -> 1;
    };
  }
}
