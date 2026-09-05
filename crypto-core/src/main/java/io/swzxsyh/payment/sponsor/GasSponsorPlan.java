package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.gas.GasPayerMode;

/** Gas 代付计划，告诉调用方当前链路是否可由平台代付以及需要用户做什么动作。 */
public record GasSponsorPlan(
    boolean enabled,
    boolean available,
    String providerId,
    GasPayerMode payerMode,
    String reason,
    boolean requiresUserSignature,
    String signatureScheme,
    String payload
) {

  public static GasSponsorPlan unavailable(String providerId, String reason) {
    return new GasSponsorPlan(false, false, providerId, GasPayerMode.CUSTOMER_PAYS, reason, false, null, null);
  }
}
