package io.swzxsyh.payment.orchestration;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 网关相关工作流描述贡献者。 */
@Component
@Order(20)
public class GatewayWorkflowContributor implements WorkflowDescriptorContributor {

  private final CryptoPaymentProperties properties;

  public GatewayWorkflowContributor(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public WorkflowDescriptor describe() {
    boolean enabled = properties.getGateway().isEnabled();
    return new WorkflowDescriptor(
        "gateway-order",
        "Gateway Order Composition",
        "Deducts merchant service fee first, then creates the downstream crypto order.",
        enabled,
        20,
        List.of(
            new WorkflowStageDescriptor("service-fee-deduction", "Charge merchant service fee from the gateway ledger."),
            new WorkflowStageDescriptor("create-downstream-order", "Create the downstream crypto order and cashier link.")
        ),
        List.of("feeReceipt", "createOrderResponse")
    );
  }
}
