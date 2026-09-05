package io.swzxsyh.payment.sponsor;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Gas 代付 provider 注册表，按 provider 自身 supports 结果选择可执行实现。 */
@Service
public class GasSponsorRegistry {

  private final List<GasSponsorProvider> providers;

  public GasSponsorRegistry(List<GasSponsorProvider> providers) {
    this.providers = providers.stream()
        .sorted(Comparator.comparing(GasSponsorProvider::providerId))
        .toList();
  }

  public GasSponsorPlan plan(GasSponsorContext context) {
    return providers.stream()
        .filter(provider -> provider.supports(context))
        .findFirst()
        .map(provider -> provider.plan(context))
        .orElseGet(() -> GasSponsorPlan.unavailable("none", "no gas sponsor provider supports this payment"));
  }

  public GasSponsorExecutionResult execute(String providerId, GasSponsorExecutionCommand command) {
    return providers.stream()
        .filter(provider -> provider.providerId().equalsIgnoreCase(providerId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Gas sponsor provider not found: " + providerId))
        .execute(command);
  }
}
