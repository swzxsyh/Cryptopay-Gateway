package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerScannerCheckpointRequest;
import io.swzxsyh.manager.application.ManagerScannerApplicationService;
import io.swzxsyh.payment.persistence.entity.ChainScannerCheckpoint;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/scanners")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SCANNER_MANAGE)")
public class ManagerScannerController {

  private final ManagerScannerApplicationService scannerService;

  public ManagerScannerController(ManagerScannerApplicationService scannerService) {
    this.scannerService = scannerService;
  }

  /** 分页查询链扫描 checkpoint。 */
  @GetMapping("/checkpoints")
  public ManagerApiResponse<ManagerPageResponse<ChainScannerCheckpoint>> checkpoints(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String chain) {
    return ManagerApiResponse.ok(scannerService.pageScannerCheckpoints(page, size, chain));
  }

  /** 新增或调整链扫描 checkpoint。 */
  @PostMapping("/checkpoints")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SCANNER_CHECKPOINT_SAVE)")
  public ManagerApiResponse<ChainScannerCheckpoint> saveCheckpoint(
      @RequestBody ManagerScannerCheckpointRequest request) {
    return ManagerApiResponse.ok(scannerService.saveScannerCheckpoint(request));
  }
}
