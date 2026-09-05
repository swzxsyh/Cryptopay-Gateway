package io.swzxsyh.payment.reconciliation;

/** 单次对账运行汇总。 */
public record PaymentReconciliationSummary(
    int orderChecked,
    int rawLogChecked,
    int matched,
    int warning,
    int mismatch) {}
