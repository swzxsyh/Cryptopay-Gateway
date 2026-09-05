package io.swzxsyh.payment.chain.status;

/** 单链交易状态解析策略。新增链时新增实现，payment 入账链路无需理解链细节。 */
public interface ChainTransactionStatusResolver {

  /** 是否支持当前链编码。 */
  boolean supports(String chain);

  /** 解析交易当前状态。 */
  ChainTransactionStatusResult resolve(ChainTransactionStatusContext context);
}
