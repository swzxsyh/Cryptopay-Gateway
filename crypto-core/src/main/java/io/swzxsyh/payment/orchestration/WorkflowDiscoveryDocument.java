package io.swzxsyh.payment.orchestration;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowDiscoveryDocument(
    String serviceName,
    String publicBaseUrl,
    boolean enabled,
    LocalDateTime generatedAt,
    List<WorkflowDescriptor> workflows
) {
}
