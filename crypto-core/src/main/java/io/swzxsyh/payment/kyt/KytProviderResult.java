package io.swzxsyh.payment.kyt;

import java.time.LocalDateTime;
import java.util.List;

/** 单个 KYT 提供方的返回结果。 */
public record KytProviderResult(
    String providerId,
    KytDecision decision,
    int riskScore,
    List<String> reasons,
    List<String> matchedSignals,
    LocalDateTime checkedAt
) {
}
