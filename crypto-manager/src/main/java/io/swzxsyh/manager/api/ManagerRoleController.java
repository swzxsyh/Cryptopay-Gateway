package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.DataPermissionSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.FunctionPermissionOption;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.FunctionPermissionTreeNode;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.FunctionPermissionSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.RoleDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.RoleSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.RoleWithFunctionPermissionsSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.UserRoleAssignRequest;
import io.swzxsyh.manager.application.ManagerRoleApplicationService;
import io.swzxsyh.manager.security.entity.ManagerRole;
import io.swzxsyh.manager.security.entity.ManagerRoleDataPermission;
import io.swzxsyh.manager.security.entity.ManagerRoleFunctionPermission;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端角色、功能权限、数据权限和用户角色绑定接口。 */
@RestController
@RequestMapping("/manager/roles")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_MANAGE)")
public class ManagerRoleController {

  private final ManagerRoleApplicationService roleService;

  public ManagerRoleController(ManagerRoleApplicationService roleService) {
    this.roleService = roleService;
  }

  /** 分页查询角色。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<ManagerRole>> roles(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(roleService.pageRoles(page, size, enabled));
  }

  /** 查询角色详情。 */
  @GetMapping("/{roleCode}")
  public ManagerApiResponse<RoleDetailResponse> roleDetail(@PathVariable String roleCode) {
    return ManagerApiResponse.ok(roleService.roleDetail(roleCode));
  }

  /** 新增或更新角色。 */
  @PostMapping
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ROLE_SAVE)")
  public ManagerApiResponse<ManagerRole> saveRole(@RequestBody RoleSaveRequest request) {
    return ManagerApiResponse.ok(roleService.saveRole(request));
  }

  /** 查询可勾选的功能权限菜单。 */
  @GetMapping("/function-permission-options")
  public ManagerApiResponse<List<FunctionPermissionOption>> functionPermissionOptions() {
    return ManagerApiResponse.ok(roleService.functionPermissionOptions());
  }

  /** 查询可勾选的功能权限菜单树。 */
  @GetMapping("/function-permission-tree")
  public ManagerApiResponse<List<FunctionPermissionTreeNode>> functionPermissionTree() {
    return ManagerApiResponse.ok(roleService.functionPermissionTree());
  }

  /** 新增或更新角色，并批量覆盖该角色的功能权限。 */
  @PostMapping("/with-function-permissions")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ROLE_SAVE)")
  public ManagerApiResponse<RoleDetailResponse> saveRoleWithFunctionPermissions(
      @RequestBody RoleWithFunctionPermissionsSaveRequest request) {
    return ManagerApiResponse.ok(roleService.saveRoleWithFunctionPermissions(request));
  }

  /** 新增或更新角色功能权限。 */
  @PostMapping("/{roleCode}/function-permissions")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ROLE_SAVE)")
  public ManagerApiResponse<ManagerRoleFunctionPermission> saveFunctionPermission(
      @PathVariable String roleCode,
      @RequestBody FunctionPermissionSaveRequest request) {
    return ManagerApiResponse.ok(roleService.saveFunctionPermission(roleCode, request));
  }

  /** 新增或更新角色数据权限。 */
  @PostMapping("/{roleCode}/data-permissions")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ROLE_SAVE)")
  public ManagerApiResponse<ManagerRoleDataPermission> saveDataPermission(
      @PathVariable String roleCode,
      @RequestBody DataPermissionSaveRequest request) {
    return ManagerApiResponse.ok(roleService.saveDataPermission(roleCode, request));
  }

  /** 给指定管理员账号重新分配角色。 */
  @PostMapping("/users/{userId}")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).USER_SAVE)")
  public ManagerApiResponse<Void> assignUserRoles(
      @PathVariable Long userId,
      @RequestBody UserRoleAssignRequest request) {
    roleService.assignUserRoles(userId, request);
    return ManagerApiResponse.ok(null);
  }
}
