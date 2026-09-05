package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerReconciliationDtos.HandleRequest;
import io.swzxsyh.manager.api.dto.ManagerReconciliationDtos.RunRequest;
import io.swzxsyh.manager.application.ManagerReconciliationApplicationService;
import io.swzxsyh.payment.persistence.entity.PaymentReconciliationRecord;
import io.swzxsyh.payment.reconciliation.PaymentReconciliationSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端支付对账接口。 */
@RestController
@RequestMapping("/manager/reconciliations")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RECONCILIATION_MANAGE)")
public class ManagerReconciliationController {

  private final ManagerReconciliationApplicationService reconciliationService;

  public ManagerReconciliationController(ManagerReconciliationApplicationService reconciliationService) {
    this.reconciliationService = reconciliationService;
  }

  /** 手动触发一批对账。 */
  @PostMapping("/run")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RECONCILIATION_RUN)")
  public ManagerApiResponse<PaymentReconciliationSummary> run(
      @RequestBody(required = false) RunRequest request) {
    return ManagerApiResponse.ok(reconciliationService.run(request));
  }

  /** 分页查询对账结果。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<PaymentReconciliationRecord>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String cryptoOrderNo,
      @RequestParam(required = false) String chain,
      @RequestParam(required = false) String token,
      @RequestParam(required = false) String reconcileStatus,
      @RequestParam(required = false) String issueType,
      @RequestParam(required = false) String txHash) {
    return ManagerApiResponse.ok(
        reconciliationService.page(
            page, size, merchantId, cryptoOrderNo, chain, token, reconcileStatus, issueType, txHash));
  }

  /** 人工确认对账结果。 */
  @PostMapping("/{id}/manual-confirm")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RECONCILIATION_MANUAL_CONFIRM)")
  public ManagerApiResponse<PaymentReconciliationRecord> manualConfirm(
      @PathVariable Long id,
      @RequestBody(required = false) HandleRequest request) {
    return ManagerApiResponse.ok(reconciliationService.manualConfirm(id, request));
  }
}
