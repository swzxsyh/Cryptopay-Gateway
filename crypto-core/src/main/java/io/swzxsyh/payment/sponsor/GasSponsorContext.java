package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.gas.GasPolicyResult;
import io.swzxsyh.payment.routing.TokenRoutePlan;

/** Gas 代付规划上下文，聚合订单、用户选择、Token 路由和 Gas 预判结果。 */
public record GasSponsorContext(
    PaymentOrder order,
    PaymentSelection selection,
    TokenRoutePlan tokenRoutePlan,
    GasPolicyResult gasPolicyResult
) {
}
