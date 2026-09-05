package io.swzxsyh.payment.discovery.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.orchestration.WorkflowCatalogService;
import io.swzxsyh.payment.orchestration.WorkflowDiscoveryDocument;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 工作流自描述接口。 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/discovery")
public class WorkflowDiscoveryController {

  private final CryptoPaymentProperties properties;
  private final WorkflowCatalogService workflowCatalogService;

  public WorkflowDiscoveryController(
      CryptoPaymentProperties properties,
      WorkflowCatalogService workflowCatalogService) {
    this.properties = properties;
    this.workflowCatalogService = workflowCatalogService;
  }

  /** 返回当前系统可用工作流。 */
  @GetMapping
  public ApiResponse<WorkflowDiscoveryDocument> discover() {
    WorkflowDiscoveryDocument document = new WorkflowDiscoveryDocument(
            "CryptoPay Gateway",
        properties.getDiscovery().getPublicBaseUrl(),
        properties.getDiscovery().isEnabled(),
        LocalDateTime.now(),
        workflowCatalogService.listWorkflows()
    );
    log.info("Generated workflow discovery document. enabled={}, workflowCount={}",
        document.enabled(), document.workflows().size());
    return ApiResponse.ok(document);
  }
}
