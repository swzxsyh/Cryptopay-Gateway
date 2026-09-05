package io.swzxsyh.payment.channel.contract;

import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;

/** 托管钱包合约地址策略。 */
public interface SettlementContractAddressStrategy {

  boolean supports(PaymentOrder order, PaymentSelection selection);

  ContractAddressPlan resolve(PaymentOrder order, PaymentSelection selection);

  String mode();
}
