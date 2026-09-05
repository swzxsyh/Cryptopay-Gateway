package io.swzxsyh.payment.gas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GasPolicyResult(
    boolean enabled,
    GasPayerMode payerMode,
    String providerId,
    boolean platformBalanceSufficient,
    boolean customerBalanceSufficient,
    BigDecimal estimatedGasLimit,
    BigDecimal estimatedGasPriceWei,
    BigDecimal estimatedNativeFeeWei,
    BigDecimal walletBalanceWei,
    BigDecimal treasuryBalanceWei,
    String reason,
    String fallbackSuggestion,
    List<String> reasons,
    LocalDateTime checkedAt
) {
}
