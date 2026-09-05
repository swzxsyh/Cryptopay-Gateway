package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerAuditApplicationService;
import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/audits")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).AUDIT_VIEW)")
public class ManagerAuditController {

  private final ManagerAuditApplicationService auditService;

  public ManagerAuditController(ManagerAuditApplicationService auditService) {
    this.auditService = auditService;
  }

  /** 分页查询审计记录。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<PaymentAuditRecord>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String bizType,
      @RequestParam(required = false) String bizKey,
      @RequestParam(required = false) String merchantId) {
    return ManagerApiResponse.ok(
        auditService.pageAudits(page, size, eventType, bizType, bizKey, merchantId));
  }
}
