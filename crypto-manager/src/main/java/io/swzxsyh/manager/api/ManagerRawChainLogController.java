package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerRawChainLogHandleRequest;
import io.swzxsyh.manager.application.ManagerRawChainLogApplicationService;
import io.swzxsyh.payment.persistence.entity.RawChainLog;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端原始链上流水接口。 */
@RestController
@RequestMapping("/manager/raw-chain-logs")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RAW_CHAIN_LOG_MANAGE)")
public class ManagerRawChainLogController {

  private final ManagerRawChainLogApplicationService rawChainLogService;

  public ManagerRawChainLogController(ManagerRawChainLogApplicationService rawChainLogService) {
    this.rawChainLogService = rawChainLogService;
  }

  /** 分页查询原始链上流水。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<RawChainLog>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String chain,
      @RequestParam(required = false) String txHash,
      @RequestParam(required = false) String toAddress,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        rawChainLogService.pageRawChainLogs(page, size, chain, txHash, toAddress, status));
  }

  /** 人工标记原始流水已处理。 */
  @PostMapping("/{id}/manual-processed")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RAW_CHAIN_LOG_MANUAL_PROCESS)")
  public ManagerApiResponse<RawChainLog> manualProcessed(
      @PathVariable Long id, @RequestBody(required = false) ManagerRawChainLogHandleRequest request) {
    return ManagerApiResponse.ok(rawChainLogService.markRawChainLogManualProcessed(id, request));
  }
}
