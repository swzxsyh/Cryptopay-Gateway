package io.swzxsyh.payment.api.dto;

/** 前端钱包广播交易后的 txHash 上报请求。 */
public record ReportPaymentTxHashRequest(
    String txHash
) {
}
