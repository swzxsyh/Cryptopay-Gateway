package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.BlockRequest;
import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.OverviewResponse;
import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.PolicyRequest;
import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerManualActionRequest;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerAddressPoolApplicationService;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolPolicy;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolRecord;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/address-pools")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_MANAGE)")
public class ManagerAddressPoolController {

  private final ManagerAddressPoolApplicationService addressPoolService;

  public ManagerAddressPoolController(ManagerAddressPoolApplicationService addressPoolService) {
    this.addressPoolService = addressPoolService;
  }

  /** 分页查询地址池记录。 */
  @GetMapping("/records")
  public ManagerApiResponse<ManagerPageResponse<DerivedAddressPoolRecord>> records(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String poolKey,
      @RequestParam(required = false) String chain,
      @RequestParam(required = false) String token,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean riskFlag) {
    return ManagerApiResponse.ok(
        addressPoolService.pageAddressRecords(page, size, poolKey, chain, token, status, riskFlag));
  }

  /** 查询指定地址池库存概览。 */
  @GetMapping("/{poolKey}/overview")
  public ManagerApiResponse<OverviewResponse> overview(
      @PathVariable String poolKey) {
    return ManagerApiResponse.ok(addressPoolService.addressPoolOverview(poolKey));
  }

  /** 查询可归集候选地址。 */
  @GetMapping("/{poolKey}/collectable-addresses")
  public ManagerApiResponse<List<String>> collectableAddresses(
      @PathVariable String poolKey, @RequestParam(defaultValue = "100") int limit) {
    return ManagerApiResponse.ok(addressPoolService.collectableAddresses(poolKey, limit));
  }

  /** 手动释放地址租约。 */
  @PostMapping("/{poolKey}/leases/{leaseId}/release")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_RELEASE)")
  public ManagerApiResponse<Boolean> releaseLease(
      @PathVariable String poolKey,
      @PathVariable String leaseId,
      @RequestBody(required = false) ManagerManualActionRequest request) {
    String reason = request == null ? "manager manual release" : request.reason();
    return ManagerApiResponse.ok(addressPoolService.releaseAddressLease(poolKey, leaseId, reason));
  }

  /** 手动冻结风险地址。 */
  @PostMapping("/{poolKey}/addresses/{address}/block")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_BLOCK)")
  public ManagerApiResponse<Boolean> blockAddress(
      @PathVariable String poolKey,
      @PathVariable String address,
      @RequestBody(required = false) BlockRequest request) {
    return ManagerApiResponse.ok(
        addressPoolService.blockAddress(poolKey, address, request));
  }

  /** 手动退休地址。 */
  @PostMapping("/{poolKey}/addresses/{address}/retire")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_RETIRE)")
  public ManagerApiResponse<DerivedAddressPoolRecord> retireAddress(
      @PathVariable String poolKey,
      @PathVariable String address,
      @RequestBody(required = false) ManagerManualActionRequest request) {
    String reason = request == null ? "manager manual retire" : request.reason();
    return ManagerApiResponse.ok(addressPoolService.retireAddress(poolKey, address, reason));
  }

  /** 手动恢复地址为可用态，并重新写入 Redis 可用队列。 */
  @PostMapping("/{poolKey}/addresses/{address}/restore")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_RELEASE)")
  public ManagerApiResponse<DerivedAddressPoolRecord> restoreAddress(
      @PathVariable String poolKey,
      @PathVariable String address,
      @RequestBody(required = false) ManagerManualActionRequest request) {
    String reason = request == null ? "manager manual restore" : request.reason();
    return ManagerApiResponse.ok(addressPoolService.restoreAddress(poolKey, address, reason));
  }

  /** 分页查询地址池策略。 */
  @GetMapping("/policies")
  public ManagerApiResponse<ManagerPageResponse<DerivedAddressPoolPolicy>> policies(
      @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(addressPoolService.pageAddressPolicies(page, size));
  }

  /** 新增或更新地址池策略。 */
  @PostMapping("/policies")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_POLICY_SAVE)")
  public ManagerApiResponse<DerivedAddressPoolPolicy> savePolicy(
      @RequestBody PolicyRequest request) {
    return ManagerApiResponse.ok(addressPoolService.saveAddressPolicy(request));
  }
}
