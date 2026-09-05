package io.swzxsyh.manager.api.dto;

import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import java.time.LocalDateTime;
import java.util.List;

/** 管理端用户维护 DTO。 */
public final class ManagerUserDtos {

  private ManagerUserDtos() {}

  /** 用户列表行，避免把 passwordHash 暴露给前端。 */
  public record UserSummaryResponse(
      Long id,
      String username,
      String displayName,
      String contactType,
      String contactValue,
      Boolean enabled,
      Boolean accountNonLocked,
      Integer failedLoginCount,
      LocalDateTime lastLoginAt,
      List<String> roleCodes,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  /** 用户详情，包含已绑定角色和由角色汇总得到的有效权限。 */
  public record UserDetailResponse(
      UserSummaryResponse user,
      List<RoleOption> roleOptions,
      List<String> functionPermissions,
      List<DataScopeRequest> userDataPermissions,
      List<DataScopeRequest> dataPermissions) {}

  /** 可勾选的角色选项。 */
  public record RoleOption(String roleCode, String roleName, Boolean superAdmin) {}

  /** 用户创建时的数据权限项。 */
  public record DataScopeRequest(String scopeType, String scopeValue) {}

  /** 三步向导最终提交的用户创建请求。 */
  public record UserCreateRequest(
      String username,
      String password,
      String displayName,
      String contactType,
      String contactValue,
      Boolean enabled,
      Boolean accountNonLocked,
      List<String> roleCodes,
      List<DataScopeRequest> dataPermissions) {}

  /** 用户基础信息更新请求。 */
  public record UserBasicUpdateRequest(
      String displayName,
      String password,
      String contactType,
      String contactValue,
      Boolean enabled,
      Boolean accountNonLocked) {}

  /** 单独覆盖用户角色请求。 */
  public record UserRoleUpdateRequest(List<String> roleCodes) {}

  /** 单独覆盖用户数据权限请求。 */
  public record UserDataPermissionUpdateRequest(List<DataScopeRequest> dataPermissions) {}

  public static UserSummaryResponse summary(ManagerAdminUser user, List<String> roleCodes) {
    return new UserSummaryResponse(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getContactType(),
        user.getContactValue(),
        user.getEnabled(),
        user.getAccountNonLocked(),
        user.getFailedLoginCount(),
        user.getLastLoginAt(),
        roleCodes,
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
