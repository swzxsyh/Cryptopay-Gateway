package io.swzxsyh.payment.orchestration;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 路由相关工作流描述贡献者。 */
@Component
@Order(10)
public class RouteWorkflowContributor implements WorkflowDescriptorContributor {

  @Override
  public WorkflowDescriptor describe() {
    return new WorkflowDescriptor(
        "token-routing",
        "Token Route Planning",
        "Detects wallet capability and token capability, then selects the best payment route.",
        true,
        10,
        List.of(
            new WorkflowStageDescriptor("wallet-capability-detect", "Detect wallet account type and smart-account capability."),
            new WorkflowStageDescriptor("token-capability-resolve", "Resolve token contract support from config or on-chain probe."),
            new WorkflowStageDescriptor("route-planner", "Choose authorization, permit, approve, or smart contract settlement."),
            new WorkflowStageDescriptor("cashier-preview", "Return a cashier-facing preview payload.")
        ),
        List.of("routeType", "routeReason", "payload")
    );
  }
}
