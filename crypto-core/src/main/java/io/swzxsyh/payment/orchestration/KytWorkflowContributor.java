package io.swzxsyh.payment.orchestration;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** KYT 相关工作流描述贡献者。 */
@Component
@Order(15)
public class KytWorkflowContributor implements WorkflowDescriptorContributor {

  @Override
  public WorkflowDescriptor describe() {
    return new WorkflowDescriptor(
        "kyt-screening",
        "KYT Risk Screening",
        "Screens payer, payee, chain and token signals before downstream payment creation.",
        true,
        15,
        List.of(
            new WorkflowStageDescriptor("address-normalization", "Normalize payer and payee addresses."),
            new WorkflowStageDescriptor("local-rule-screening", "Apply local allowlist/denylist and simple risk heuristics."),
            new WorkflowStageDescriptor("provider-screening", "Run one or more KYT provider adapters."),
            new WorkflowStageDescriptor("decision-assembly", "Return approve, review, or reject with reasons.")
        ),
        List.of("decision", "riskScore", "providerResults", "reasons")
    );
  }
}
