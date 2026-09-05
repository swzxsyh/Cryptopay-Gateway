package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.RoleOption;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserBasicUpdateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserCreateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserDataPermissionUpdateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserRoleUpdateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserSummaryResponse;
import io.swzxsyh.manager.application.ManagerUserApplicationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端人员维护接口。 */
@RestController
@RequestMapping("/manager/users")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_MANAGE)")
public class ManagerUserController {

  private final ManagerUserApplicationService userService;

  public ManagerUserController(ManagerUserApplicationService userService) {
    this.userService = userService;
  }

  /** 分页查询管理端人员。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<UserSummaryResponse>> users(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(userService.pageUsers(page, size, username, enabled));
  }

  /** 查询管理端人员详情。 */
  @GetMapping("/{userId}")
  public ManagerApiResponse<UserDetailResponse> detail(@PathVariable Long userId) {
    return ManagerApiResponse.ok(userService.userDetail(userId));
  }

  /** 查询可分配角色选项，角色本身已绑定功能权限和数据权限。 */
  @GetMapping("/role-options")
  public ManagerApiResponse<List<RoleOption>> roleOptions() {
    return ManagerApiResponse.ok(userService.roleOptions());
  }

  /** 三步向导最终提交创建人员。 */
  @PostMapping
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).USER_SAVE)")
  public ManagerApiResponse<UserDetailResponse> create(@RequestBody UserCreateRequest request) {
    return ManagerApiResponse.ok(userService.createUser(request));
  }

  /** 单独更新人员基础信息。 */
  @PostMapping("/{userId}/basic")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).USER_SAVE)")
  public ManagerApiResponse<UserDetailResponse> updateBasic(
      @PathVariable Long userId,
      @RequestBody UserBasicUpdateRequest request) {
    return ManagerApiResponse.ok(userService.updateBasic(userId, request));
  }

  /** 单独覆盖人员角色，功能权限和数据权限由角色自动联动。 */
  @PostMapping("/{userId}/roles")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).USER_SAVE)")
  public ManagerApiResponse<UserDetailResponse> updateRoles(
      @PathVariable Long userId,
      @RequestBody UserRoleUpdateRequest request) {
    return ManagerApiResponse.ok(userService.updateRoles(userId, request));
  }

  /** 单独覆盖人员数据权限，主要用于绑定可访问商户。 */
  @PostMapping("/{userId}/data-permissions")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).USER_SAVE)")
  public ManagerApiResponse<UserDetailResponse> updateDataPermissions(
      @PathVariable Long userId,
      @RequestBody UserDataPermissionUpdateRequest request) {
    return ManagerApiResponse.ok(userService.updateDataPermissions(userId, request));
  }
}
