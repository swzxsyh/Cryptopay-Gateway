package io.swzxsyh.payment.persistence.entity;

/** 派生地址池状态。 */
public enum DerivedAddressPoolStatus {
  AVAILABLE,
  LEASED,
  COOLDOWN,
  RISK_BLOCKED,
  RETIRED
}
