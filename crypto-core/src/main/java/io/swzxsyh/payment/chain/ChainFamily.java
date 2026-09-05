package io.swzxsyh.payment.chain;

/** 链族分类，用于区分 EVM、Solana 等不同 RPC/交易模型。 */
public enum ChainFamily {
  EVM,
  TRON,
  SUI,
  TON,
  SOLANA,
  UNKNOWN
}
