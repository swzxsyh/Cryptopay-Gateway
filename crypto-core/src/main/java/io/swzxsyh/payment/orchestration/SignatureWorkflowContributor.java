package io.swzxsyh.payment.orchestration;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 签名相关工作流描述贡献者。 */
@Component
@Order(5)
public class SignatureWorkflowContributor implements WorkflowDescriptorContributor {

  private final CryptoPaymentProperties properties;

  public SignatureWorkflowContributor(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public WorkflowDescriptor describe() {
    boolean enabled = properties.getSignature().isEnabled();
    return new WorkflowDescriptor(
        "signature-guard",
        "Request Signature Guard",
        "Verifies inbound merchant requests and signs outbound callbacks.",
        enabled,
        5,
        List.of(
            new WorkflowStageDescriptor("request-canonicalization", "Serialize request args into a stable canonical payload."),
            new WorkflowStageDescriptor("request-verification", "Validate timestamp, nonce, and merchant signature from the database."),
            new WorkflowStageDescriptor("callback-signing", "Attach platform signature headers to callback payloads.")
        ),
        List.of("merchantId", "timestamp", "nonce", "signature")
    );
  }
}
