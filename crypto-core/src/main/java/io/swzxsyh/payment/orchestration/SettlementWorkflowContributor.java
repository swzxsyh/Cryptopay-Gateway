package io.swzxsyh.payment.orchestration;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 结算相关工作流描述贡献者。 */
@Component
@Order(30)
public class SettlementWorkflowContributor implements WorkflowDescriptorContributor {

  private final CryptoPaymentProperties properties;

  public SettlementWorkflowContributor(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public WorkflowDescriptor describe() {
    boolean enabled = properties.getContract().isEnabled();
    return new WorkflowDescriptor(
        "contract-settlement",
        "Contract Settlement Assembly",
        "Validates settlement rules, splits the amount, and assembles the contract call payload.",
        enabled,
        30,
        List.of(
            new WorkflowStageDescriptor("rule-validation", "Validate settlement rules and receiver completeness."),
            new WorkflowStageDescriptor("amount-splitting", "Split the payment amount by basis points."),
            new WorkflowStageDescriptor("payload-composition", "Compose a signing payload and readable contract call data."),
            new WorkflowStageDescriptor("signature-adapter", "Route signing to the configured signer adapter.")
        ),
        List.of("signPayload", "signature", "contractCallData")
    );
  }
}
