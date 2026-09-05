package io.swzxsyh.payment.orchestration;

import java.util.List;

/** 工作流描述。 */
public record WorkflowDescriptor(
    String id,
    String title,
    String description,
    boolean enabled,
    int order,
    List<WorkflowStageDescriptor> stages,
    List<String> outputs
) {
}
