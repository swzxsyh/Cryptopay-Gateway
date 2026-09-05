package io.swzxsyh.manager.api.dto;

import io.swzxsyh.manager.security.entity.ManagerRole;
import io.swzxsyh.manager.security.entity.ManagerRoleDataPermission;
import io.swzxsyh.manager.security.entity.ManagerRoleFunctionPermission;
import java.util.List;

/** 角色权限相关 DTO 聚合，减少 manager 模块中零散的小 record 文件。 */
public final class ManagerRoleDtos {

  private ManagerRoleDtos() {}

  /** 管理端角色保存请求。 */
  public record RoleSaveRequest(
      String roleCode,
      String roleName,
      Boolean superAdmin,
      Boolean enabled) {}

  /** 角色保存请求，附带本次勾选的功能权限编码。 */
  public record RoleWithFunctionPermissionsSaveRequest(
      String roleCode,
      String roleName,
      Boolean superAdmin,
      Boolean enabled,
      List<String> functionPermissionCodes) {}

  /** 前端勾选菜单使用的功能权限选项。 */
  public record FunctionPermissionOption(
      String permissionCode,
      String permissionName) {}

  /** 前端角色菜单树节点；父节点仅用于分组，叶子节点才是真正的权限编码。 */
  public record FunctionPermissionTreeNode(
      String key,
      String label,
      String permissionCode,
      List<FunctionPermissionTreeNode> children) {}

  /** 管理端角色功能权限保存请求。 */
  public record FunctionPermissionSaveRequest(
      String permissionCode,
      String permissionName,
      Boolean enabled) {}

  /** 管理端角色数据权限保存请求。 */
  public record DataPermissionSaveRequest(
      String scopeType,
      String scopeValue,
      Boolean enabled) {}

  /** 管理端用户角色分配请求。 */
  public record UserRoleAssignRequest(List<String> roleCodes) {}

  /** 管理端角色详情响应，包含功能权限和数据权限。 */
  public record RoleDetailResponse(
      ManagerRole role,
      List<ManagerRoleFunctionPermission> functionPermissions,
      List<ManagerRoleDataPermission> dataPermissions) {}
}
