package io.swzxsyh.payment.domain;

/** 智能合约分账状态。 */
public enum ContractSettlementStatus {
  PLANNED,
  SUBMITTED,
  ISOLATED,
  SETTLED,
  RELEASED,
  FAILED
}
