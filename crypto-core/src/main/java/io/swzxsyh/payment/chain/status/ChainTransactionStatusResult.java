package io.swzxsyh.payment.chain.status;

/** 链上交易状态解析结果，payment 只看统一状态，不关心底层链的差异。 */
public record ChainTransactionStatusResult(
    ChainTransactionStatus status,
    int currentConfirmations,
    Long blockNumber,
    String reason) {

  public static ChainTransactionStatusResult unsupported(String reason) {
    return new ChainTransactionStatusResult(ChainTransactionStatus.UNSUPPORTED, 0, null, reason);
  }

  public boolean readyForPayment() {
    return status == ChainTransactionStatus.CONFIRMED;
  }
}
