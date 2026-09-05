package io.swzxsyh.payment.settlement;

/** 分账规则定义，通常来自商户配置或订单参数。 */
public record SettlementRule(
    String role,
    String receiver,
    int basisPoints
) {
}
