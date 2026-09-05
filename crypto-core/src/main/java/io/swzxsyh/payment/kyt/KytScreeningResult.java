package io.swzxsyh.payment.kyt;

import java.time.LocalDateTime;
import java.util.List;

/** KYT 风控结果。 */
public record KytScreeningResult(
    boolean enabled,
    KytDecision decision,
    int riskScore,
    String selectedProvider,
    List<KytProviderResult> providerResults,
    List<String> reasons,
    LocalDateTime checkedAt
) {
}
