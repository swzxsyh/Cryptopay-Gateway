package io.swzxsyh.manager.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.manager.security.ManagerAuthority;
import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import io.swzxsyh.manager.security.entity.ManagerRole;
import io.swzxsyh.manager.security.entity.ManagerRoleDataPermission;
import io.swzxsyh.manager.security.entity.ManagerUserDataPermission;
import io.swzxsyh.manager.security.entity.ManagerUserRole;
import io.swzxsyh.manager.security.mapper.ManagerAdminUserMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleDataPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserDataPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserRoleMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 管理端权限判断服务，供 @PreAuthorize 和业务层数据权限过滤使用。 */
@Service("managerPermissionService")
public class ManagerPermissionService {

  public static final String DATA_SCOPE_ALL = "ALL";
  public static final String DATA_SCOPE_MERCHANT = "MERCHANT";
  public static final String DATA_SCOPE_CHAIN = "CHAIN";

  private final ManagerAdminUserMapper adminUserMapper;
  private final ManagerRoleMapper roleMapper;
  private final ManagerUserRoleMapper userRoleMapper;
  private final ManagerRoleDataPermissionMapper dataPermissionMapper;
  private final ManagerUserDataPermissionMapper userDataPermissionMapper;

  public ManagerPermissionService(
      ManagerAdminUserMapper adminUserMapper,
      ManagerRoleMapper roleMapper,
      ManagerUserRoleMapper userRoleMapper,
      ManagerRoleDataPermissionMapper dataPermissionMapper,
      ManagerUserDataPermissionMapper userDataPermissionMapper) {
    this.adminUserMapper = adminUserMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.dataPermissionMapper = dataPermissionMapper;
    this.userDataPermissionMapper = userDataPermissionMapper;
  }

  /** 判断当前登录人是否拥有某个功能权限；ADMIN 或 super_admin 角色直接放行。 */
  public boolean hasFunction(Authentication authentication, String permissionCode) {
    if (!authenticated(authentication) || !StringUtils.hasText(permissionCode)) {
      return false;
    }
    if (isAdmin(authentication)) {
      return true;
    }
    String expected = ManagerAuthority.permission(permissionCode);
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(expected::equals);
  }

  /** 判断当前登录人是否可访问指定数据范围；ADMIN 或 super_admin 角色直接放行。 */
  public boolean hasData(Authentication authentication, String scopeType, String scopeValue) {
    if (!authenticated(authentication) || !StringUtils.hasText(scopeType)) {
      return false;
    }
    if (isAdmin(authentication)) {
      return true;
    }
    String normalizedType = scopeType.trim().toUpperCase();
    String normalizedValue = scopeValue == null ? "" : scopeValue.trim();
    ManagerAdminUser user = findUser(authentication.getName());
    if (user == null) {
      return false;
    }
    for (String roleCode : roleCodes(user)) {
      List<ManagerRoleDataPermission> permissions =
          dataPermissionMapper.selectList(
              new LambdaQueryWrapper<ManagerRoleDataPermission>()
                  .eq(ManagerRoleDataPermission::getRoleCode, roleCode)
                  .eq(ManagerRoleDataPermission::getEnabled, Boolean.TRUE));
      for (ManagerRoleDataPermission permission : permissions) {
        if (DATA_SCOPE_ALL.equalsIgnoreCase(permission.getScopeType())) {
          return true;
        }
        if (normalizedType.equalsIgnoreCase(permission.getScopeType())
            && normalizedValue.equalsIgnoreCase(permission.getScopeValue())) {
          return true;
        }
      }
    }
    for (ManagerUserDataPermission permission : userDataPermissions(user.getId())) {
      if (DATA_SCOPE_ALL.equalsIgnoreCase(permission.getScopeType())) {
        return true;
      }
      if (normalizedType.equalsIgnoreCase(permission.getScopeType())
          && normalizedValue.equalsIgnoreCase(permission.getScopeValue())) {
        return true;
      }
    }
    return false;
  }

  /** 当前用户可访问的商户号；空集合表示没有商户维度数据权限，ADMIN 不应调用该结果做过滤。 */
  public Set<String> merchantScopes(String username) {
    Set<String> scopes = new LinkedHashSet<>();
    ManagerAdminUser user = findUser(username);
    if (user == null) {
      return scopes;
    }
    for (String roleCode : roleCodes(user)) {
      dataPermissionMapper
          .selectList(
              new LambdaQueryWrapper<ManagerRoleDataPermission>()
                  .eq(ManagerRoleDataPermission::getRoleCode, roleCode)
                  .eq(ManagerRoleDataPermission::getEnabled, Boolean.TRUE)
                  .eq(ManagerRoleDataPermission::getScopeType, DATA_SCOPE_MERCHANT))
          .forEach(permission -> {
            if (StringUtils.hasText(permission.getScopeValue())) {
              scopes.add(permission.getScopeValue().trim());
            }
          });
    }
    userDataPermissionMapper
        .selectList(
            new LambdaQueryWrapper<ManagerUserDataPermission>()
                .eq(ManagerUserDataPermission::getUserId, user.getId())
                .eq(ManagerUserDataPermission::getEnabled, Boolean.TRUE)
                .eq(ManagerUserDataPermission::getScopeType, DATA_SCOPE_MERCHANT))
        .forEach(permission -> {
          if (StringUtils.hasText(permission.getScopeValue())) {
            scopes.add(permission.getScopeValue().trim());
          }
        });
    return scopes;
  }

  public boolean isAdmin(Authentication authentication) {
    if (!authenticated(authentication)) {
      return false;
    }
    if (authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(ManagerAuthority.role(ManagerAuthority.ADMIN_ROLE)::equals)) {
      return true;
    }
    ManagerAdminUser user = findUser(authentication.getName());
    return user != null && roleCodes(user).stream().anyMatch(this::isSuperAdminRole);
  }

  private boolean isSuperAdminRole(String roleCode) {
    if (!StringUtils.hasText(roleCode)) {
      return false;
    }
    if (ManagerAuthority.ADMIN_ROLE.equalsIgnoreCase(roleCode)) {
      return true;
    }
    ManagerRole role =
        roleMapper.selectOne(
            new LambdaQueryWrapper<ManagerRole>()
                .eq(ManagerRole::getRoleCode, roleCode)
                .eq(ManagerRole::getEnabled, Boolean.TRUE)
                .last("limit 1"));
    return role != null && Boolean.TRUE.equals(role.getSuperAdmin());
  }

  private Set<String> roleCodes(ManagerAdminUser user) {
    Set<String> roleCodes = new LinkedHashSet<>();
    if (user == null) {
      return roleCodes;
    }
    userRoleMapper
        .selectList(
            new LambdaQueryWrapper<ManagerUserRole>()
                .eq(ManagerUserRole::getUserId, user.getId()))
        .forEach(binding -> addRole(roleCodes, binding.getRoleCode()));
    if (StringUtils.hasText(user.getRoles())) {
      for (String role : user.getRoles().split(",")) {
        addRole(roleCodes, role);
      }
    }
    return roleCodes;
  }

  private ManagerAdminUser findUser(String username) {
    if (!StringUtils.hasText(username)) {
      return null;
    }
    return adminUserMapper.selectOne(
        new LambdaQueryWrapper<ManagerAdminUser>()
            .eq(ManagerAdminUser::getUsername, username)
            .last("limit 1"));
  }

  private List<ManagerUserDataPermission> userDataPermissions(Long userId) {
    return userDataPermissionMapper.selectList(
        new LambdaQueryWrapper<ManagerUserDataPermission>()
            .eq(ManagerUserDataPermission::getUserId, userId)
            .eq(ManagerUserDataPermission::getEnabled, Boolean.TRUE));
  }

  private void addRole(Set<String> roleCodes, String roleCode) {
    if (StringUtils.hasText(roleCode)) {
      roleCodes.add(roleCode.trim().toUpperCase());
    }
  }

  private boolean authenticated(Authentication authentication) {
    return authentication != null && authentication.isAuthenticated();
  }
}
