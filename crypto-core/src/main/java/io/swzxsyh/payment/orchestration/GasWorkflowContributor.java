package io.swzxsyh.payment.orchestration;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Gas 相关工作流描述贡献者。 */
@Component
@Order(18)
public class GasWorkflowContributor implements WorkflowDescriptorContributor {

  @Override
  public WorkflowDescriptor describe() {
    return new WorkflowDescriptor(
        "gas-policy",
        "Gas Funding Policy",
        "Decides whether platform sponsors gas, customer pays gas, or a lower-fee fallback is required.",
        true,
        18,
        List.of(
            new WorkflowStageDescriptor("balance-probe", "Read native balance of customer wallet and treasury wallet."),
            new WorkflowStageDescriptor("gas-estimation", "Estimate gas limit by route type and selected payment method."),
            new WorkflowStageDescriptor("payer-selection", "Prefer platform sponsorship for hosted wallets when balance allows."),
            new WorkflowStageDescriptor("fallback-advice", "Recommend lower-fee chain or alternate payment form when balance is insufficient.")
        ),
        List.of("payerMode", "estimatedNativeFeeWei", "fallbackSuggestion", "reasons")
    );
  }
}
