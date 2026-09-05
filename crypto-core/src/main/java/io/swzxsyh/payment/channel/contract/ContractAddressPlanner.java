package io.swzxsyh.payment.channel.contract;

import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 托管钱包合约地址规划器。 */
@Slf4j
@Service
public class ContractAddressPlanner {

  private final List<SettlementContractAddressStrategy> strategies;

  public ContractAddressPlanner(List<SettlementContractAddressStrategy> strategies) {
    this.strategies = strategies;
  }

  public ContractAddressPlan resolve(PaymentOrder order, PaymentSelection selection) {
    if (order == null) {
      throw new IllegalArgumentException("order is required");
    }
    if (selection == null) {
      throw new IllegalArgumentException("selection is required");
    }
    return strategies.stream()
        .filter(strategy -> strategy.supports(order, selection))
        .findFirst()
        .map(strategy -> strategy.resolve(order, selection))
        .orElseGet(() -> {
          log.warn(
              "No contract address strategy matched, fallback to static contract address. cryptoOrderNo={}, chain={}, token={}",
              order.getCryptoOrderNo(),
              selection.chain(),
              selection.token());
          return new ContractAddressPlan("", "STATIC", null, null, null);
        });
  }

  public boolean isCreate2Enabled(PaymentOrder order, PaymentSelection selection) {
    if (order == null || selection == null) {
      return false;
    }
    return strategies.stream()
        .filter(strategy -> StringUtils.hasText(strategy.mode()) && "CREATE2".equalsIgnoreCase(strategy.mode()))
        .anyMatch(strategy -> strategy.supports(order, selection));
  }
}
