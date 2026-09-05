package io.swzxsyh.payment.orchestration;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

/** 工作流目录服务。 */
@Slf4j
@Service
public class WorkflowCatalogService {

  private final List<WorkflowDescriptorContributor> contributors;

  public WorkflowCatalogService(List<WorkflowDescriptorContributor> contributors) {
    this.contributors = new ArrayList<>(contributors);
    AnnotationAwareOrderComparator.sort(this.contributors);
  }

  public List<WorkflowDescriptor> listWorkflows() {
    return contributors.stream()
        .map(WorkflowDescriptorContributor::describe)
        .toList();
  }
}
