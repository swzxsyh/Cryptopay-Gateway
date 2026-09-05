package io.swzxsyh.payment.channel.contract;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 静态合约地址策略，作为未启用 CREATE2 时的兜底实现。 */
@Component
@Order(100)
public class StaticSettlementContractAddressStrategy implements SettlementContractAddressStrategy {

  private final CryptoPaymentProperties properties;

  public StaticSettlementContractAddressStrategy(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean supports(PaymentOrder order, PaymentSelection selection) {
    return true;
  }

  @Override
  public ContractAddressPlan resolve(PaymentOrder order, PaymentSelection selection) {
    String address = properties.getContract().getAddress();
    if (!StringUtils.hasText(address)) {
      throw new IllegalStateException("contract address is not configured");
    }
    return new ContractAddressPlan(address.trim(), mode(), null, null, null);
  }

  @Override
  public String mode() {
    return "STATIC";
  }
}
