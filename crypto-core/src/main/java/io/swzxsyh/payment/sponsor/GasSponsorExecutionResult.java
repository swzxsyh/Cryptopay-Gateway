package io.swzxsyh.payment.sponsor;

/** Gas 代付执行结果，用于把平台 relayer / fee payer 上链后的 txHash 回写订单。 */
public record GasSponsorExecutionResult(
    boolean submitted,
    String txHash,
    String providerId,
    String reason
) {
}
