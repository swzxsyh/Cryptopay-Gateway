package io.swzxsyh.manager.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.manager.security.ManagerAuthority;
import io.swzxsyh.manager.security.ManagerDataScope;
import io.swzxsyh.manager.security.ManagerPrincipal;
import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import io.swzxsyh.manager.security.entity.ManagerRole;
import io.swzxsyh.manager.security.entity.ManagerRoleDataPermission;
import io.swzxsyh.manager.security.entity.ManagerRoleFunctionPermission;
import io.swzxsyh.manager.security.entity.ManagerUserDataPermission;
import io.swzxsyh.manager.security.entity.ManagerUserRole;
import io.swzxsyh.manager.security.mapper.ManagerAdminUserMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleDataPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleFunctionPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserDataPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserRoleMapper;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 从数据库加载管理端登录账号。 */
@Service
public class ManagerAdminUserDetailsService implements UserDetailsService {

  private final ManagerAdminUserMapper adminUserMapper;
  private final ManagerRoleMapper roleMapper;
  private final ManagerUserRoleMapper userRoleMapper;
  private final ManagerRoleFunctionPermissionMapper functionPermissionMapper;
  private final ManagerRoleDataPermissionMapper dataPermissionMapper;
  private final ManagerUserDataPermissionMapper userDataPermissionMapper;

  public ManagerAdminUserDetailsService(
      ManagerAdminUserMapper adminUserMapper,
      ManagerRoleMapper roleMapper,
      ManagerUserRoleMapper userRoleMapper,
      ManagerRoleFunctionPermissionMapper functionPermissionMapper,
      ManagerRoleDataPermissionMapper dataPermissionMapper,
      ManagerUserDataPermissionMapper userDataPermissionMapper) {
    this.adminUserMapper = adminUserMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.functionPermissionMapper = functionPermissionMapper;
    this.dataPermissionMapper = dataPermissionMapper;
    this.userDataPermissionMapper = userDataPermissionMapper;
  }

  /** 根据用户名加载 Spring Security 登录用户。 */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    ManagerAdminUser user =
        adminUserMapper.selectOne(
            new LambdaQueryWrapper<ManagerAdminUser>()
                .eq(ManagerAdminUser::getUsername, username)
                .last("limit 1"));
    if (user == null) {
      throw new UsernameNotFoundException(username);
    }

    Set<String> roleCodes = loadRoleCodes(user);
    boolean admin = hasAdminRole(roleCodes);
    Set<String> functionPermissions = admin ? Set.of("*") : loadFunctionPermissions(roleCodes);
    List<ManagerDataScope> dataScopes =
        admin ? List.of(new ManagerDataScope("ALL", "*")) : loadDataScopes(user.getId(), roleCodes);
    Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
    roleCodes.forEach(role -> authorities.add(new SimpleGrantedAuthority(ManagerAuthority.role(role))));
    if (admin) {
      authorities.add(new SimpleGrantedAuthority(ManagerAuthority.role(ManagerAuthority.ADMIN_ROLE)));
    }
    functionPermissions.forEach(
        permission -> authorities.add(new SimpleGrantedAuthority(ManagerAuthority.permission(permission))));

    return new ManagerPrincipal(user, roleCodes, functionPermissions, dataScopes, authorities);
  }

  private Set<String> loadRoleCodes(ManagerAdminUser user) {
    Set<String> roleCodes = new LinkedHashSet<>();
    userRoleMapper
        .selectList(
            new LambdaQueryWrapper<ManagerUserRole>()
                .eq(ManagerUserRole::getUserId, user.getId()))
        .forEach(binding -> addRole(roleCodes, binding.getRoleCode()));
    if (StringUtils.hasText(user.getRoles())) {
      Arrays.stream(user.getRoles().split(",")).forEach(role -> addRole(roleCodes, role));
    }
    return roleCodes;
  }

  private Set<String> loadFunctionPermissions(Set<String> roleCodes) {
    Set<String> permissions = new LinkedHashSet<>();
    for (String roleCode : roleCodes) {
      functionPermissionMapper
          .selectList(
              new LambdaQueryWrapper<ManagerRoleFunctionPermission>()
                  .eq(ManagerRoleFunctionPermission::getRoleCode, roleCode)
                  .eq(ManagerRoleFunctionPermission::getEnabled, Boolean.TRUE))
          .forEach(permission -> {
            if (StringUtils.hasText(permission.getPermissionCode())) {
              permissions.add(permission.getPermissionCode().trim());
            }
          });
    }
    return permissions;
  }

  private List<ManagerDataScope> loadDataScopes(Long userId, Set<String> roleCodes) {
    List<ManagerDataScope> scopes = new java.util.ArrayList<>();
    for (String roleCode : roleCodes) {
      dataPermissionMapper
          .selectList(
              new LambdaQueryWrapper<ManagerRoleDataPermission>()
                  .eq(ManagerRoleDataPermission::getRoleCode, roleCode)
                  .eq(ManagerRoleDataPermission::getEnabled, Boolean.TRUE))
          .forEach(permission -> {
            if (StringUtils.hasText(permission.getScopeType())) {
              scopes.add(new ManagerDataScope(permission.getScopeType(), permission.getScopeValue()));
            }
          });
    }
    userDataPermissionMapper
        .selectList(
            new LambdaQueryWrapper<ManagerUserDataPermission>()
                .eq(ManagerUserDataPermission::getUserId, userId)
                .eq(ManagerUserDataPermission::getEnabled, Boolean.TRUE))
        .forEach(permission -> {
          if (StringUtils.hasText(permission.getScopeType())) {
            scopes.add(new ManagerDataScope(permission.getScopeType(), permission.getScopeValue()));
          }
        });
    return scopes;
  }

  private boolean hasAdminRole(Set<String> roleCodes) {
    for (String roleCode : roleCodes) {
      if (ManagerAuthority.ADMIN_ROLE.equalsIgnoreCase(roleCode)) {
        return true;
      }
      ManagerRole role =
          roleMapper.selectOne(
              new LambdaQueryWrapper<ManagerRole>()
                  .eq(ManagerRole::getRoleCode, roleCode)
                  .eq(ManagerRole::getEnabled, Boolean.TRUE)
                  .last("limit 1"));
      if (role != null && Boolean.TRUE.equals(role.getSuperAdmin())) {
        return true;
      }
    }
    return false;
  }

  private void addRole(Set<String> roleCodes, String roleCode) {
    if (StringUtils.hasText(roleCode)) {
      roleCodes.add(roleCode.trim().toUpperCase());
    }
  }
}
