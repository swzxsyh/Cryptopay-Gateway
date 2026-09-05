package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.domain.PaymentOrder;

/** Gas 代付执行命令；不同 provider 可以读取各自需要的授权载荷。 */
public record GasSponsorExecutionCommand(
    PaymentOrder order,
    Eip3009AuthorizationSubmission eip3009Authorization
) {
}
