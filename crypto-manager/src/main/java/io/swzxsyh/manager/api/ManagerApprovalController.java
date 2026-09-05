package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalActionRequest;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalDefinitionSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalDefinitionView;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalOptionsResponse;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalRequestView;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalStepSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalStepView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerApprovalApplicationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 通用多人审批接口，供调账等高风险操作复用。 */
@RestController
@RequestMapping("/manager/approvals")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).APPROVAL_MANAGE)")
public class ManagerApprovalController {

  private final ManagerApprovalApplicationService approvalService;

  public ManagerApprovalController(ManagerApprovalApplicationService approvalService) {
    this.approvalService = approvalService;
  }

  /** 分页查询审批单。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<ApprovalRequestView>> approvals(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String bizType,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(approvalService.pageApprovals(page, size, bizType, status));
  }

  /** 查询审批配置可选项。 */
  @GetMapping("/options")
  public ManagerApiResponse<ApprovalOptionsResponse> options() {
    return ManagerApiResponse.ok(approvalService.options());
  }

  /** 查询审批详情。 */
  @GetMapping("/{approvalNo}")
  public ManagerApiResponse<ApprovalDetailResponse> detail(@PathVariable String approvalNo) {
    return ManagerApiResponse.ok(approvalService.detail(approvalNo));
  }

  /** 分页查询审批流配置。 */
  @GetMapping("/definitions")
  public ManagerApiResponse<ManagerPageResponse<ApprovalDefinitionView>> definitions(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String bizType) {
    return ManagerApiResponse.ok(approvalService.pageDefinitions(page, size, bizType));
  }

  /** 保存审批流配置。 */
  @PostMapping("/definitions")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).APPROVAL_CONFIG_SAVE)")
  public ManagerApiResponse<ApprovalDefinitionView> saveDefinition(
      @RequestBody ApprovalDefinitionSaveRequest request) {
    return ManagerApiResponse.ok(approvalService.saveDefinition(request));
  }

  /** 查询审批步骤配置。 */
  @GetMapping("/definitions/{definitionCode}/steps")
  public ManagerApiResponse<List<ApprovalStepView>> steps(@PathVariable String definitionCode) {
    return ManagerApiResponse.ok(approvalService.steps(definitionCode));
  }

  /** 保存审批步骤配置。 */
  @PostMapping("/steps")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).APPROVAL_CONFIG_SAVE)")
  public ManagerApiResponse<ApprovalStepView> saveStep(@RequestBody ApprovalStepSaveRequest request) {
    return ManagerApiResponse.ok(approvalService.saveStep(request));
  }

  /** 审批通过。 */
  @PostMapping("/{approvalNo}/approve")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).APPROVAL_REVIEW)")
  public ManagerApiResponse<ApprovalRequestView> approve(
      @PathVariable String approvalNo, @RequestBody(required = false) ApprovalActionRequest request) {
    return ManagerApiResponse.ok(approvalService.approve(approvalNo, request));
  }

  /** 审批拒绝。 */
  @PostMapping("/{approvalNo}/reject")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).APPROVAL_REVIEW)")
  public ManagerApiResponse<ApprovalRequestView> reject(
      @PathVariable String approvalNo, @RequestBody(required = false) ApprovalActionRequest request) {
    return ManagerApiResponse.ok(approvalService.reject(approvalNo, request));
  }
}
