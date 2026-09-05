package io.swzxsyh.payment.subscription.contract;

/** 订阅链上执行结果。 */
public record SubscriptionExecutionResult(
    boolean submitted,
    String txHash,
    String message
) {

  public static SubscriptionExecutionResult skipped(String message) {
    return new SubscriptionExecutionResult(false, null, message);
  }

  public static SubscriptionExecutionResult submitted(String txHash) {
    return new SubscriptionExecutionResult(true, txHash, "submitted");
  }
}
