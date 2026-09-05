package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerSettlementDtos.DetailResponse;
import io.swzxsyh.manager.api.dto.ManagerSettlementDtos.MarkRequest;
import io.swzxsyh.manager.application.ManagerSettlementApplicationService;
import io.swzxsyh.payment.persistence.entity.ContractSettlementRecord;
import io.swzxsyh.payment.persistence.entity.ContractSettlementSplitRecord;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/settlements")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SETTLEMENT_MANAGE)")
public class ManagerSettlementController {

  private final ManagerSettlementApplicationService settlementService;

  public ManagerSettlementController(ManagerSettlementApplicationService settlementService) {
    this.settlementService = settlementService;
  }

  @GetMapping
  /** 分页查询结算记录。 */
  public ManagerApiResponse<ManagerPageResponse<ContractSettlementRecord>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String chain,
      @RequestParam(required = false) String token,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String cryptoOrderNo) {
    return ManagerApiResponse.ok(
        settlementService.pageSettlements(page, size, chain, token, status, cryptoOrderNo));
  }

  @GetMapping("/{cryptoOrderNo}")
  /** 查询结算详情。 */
  public ManagerApiResponse<DetailResponse> detail(
      @PathVariable String cryptoOrderNo) {
    return ManagerApiResponse.ok(settlementService.settlementDetail(cryptoOrderNo));
  }

  @GetMapping("/splits")
  /** 分页查询分账明细。 */
  public ManagerApiResponse<ManagerPageResponse<ContractSettlementSplitRecord>> splits(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String cryptoOrderNo,
      @RequestParam(required = false) String receiver,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        settlementService.pageSettlementSplits(page, size, cryptoOrderNo, receiver, status));
  }

  @PostMapping("/{cryptoOrderNo}/mark-isolated")
  /** 手动标记结算已隔离。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SETTLEMENT_MARK)")
  public ManagerApiResponse<ContractSettlementRecordView> markIsolated(
      @PathVariable String cryptoOrderNo, @RequestBody MarkRequest request) {
    return ManagerApiResponse.ok(
        settlementService.markSettlementIsolated(cryptoOrderNo, request.txHash(), request.blockNumber()));
  }

  @PostMapping("/{cryptoOrderNo}/mark-released")
  /** 手动标记结算已放行。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SETTLEMENT_MARK)")
  public ManagerApiResponse<ContractSettlementRecordView> markReleased(
      @PathVariable String cryptoOrderNo, @RequestBody MarkRequest request) {
    return ManagerApiResponse.ok(
        settlementService.markSettlementReleased(cryptoOrderNo, request.txHash(), request.blockNumber()));
  }

  @PostMapping("/{cryptoOrderNo}/mark-failed")
  /** 手动标记结算失败。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SETTLEMENT_MARK)")
  public ManagerApiResponse<ContractSettlementRecordView> markFailed(
      @PathVariable String cryptoOrderNo, @RequestBody MarkRequest request) {
    return ManagerApiResponse.ok(
        settlementService.markSettlementFailed(
            cryptoOrderNo, request.txHash(), request.failureReason()));
  }
}
