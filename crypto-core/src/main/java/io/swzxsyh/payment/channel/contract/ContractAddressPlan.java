package io.swzxsyh.payment.channel.contract;

/** 托管钱包合约地址解析结果。 */
public record ContractAddressPlan(
    String address,
    String mode,
    String factoryAddress,
    String salt,
    String initCodeHash
) {}
