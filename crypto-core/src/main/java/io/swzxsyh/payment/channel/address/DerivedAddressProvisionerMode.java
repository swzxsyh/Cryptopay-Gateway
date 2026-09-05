package io.swzxsyh.payment.channel.address;

/** 派生地址生成模式。 */
public enum DerivedAddressProvisionerMode {
  HD,
  SOLANA_HD,
  KEYSTORE,
  THIRD_PARTY_API,
  ADDRESS_FACTORY
}
