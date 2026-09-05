package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerUserDtos;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.DataScopeRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.RoleOption;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserBasicUpdateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserCreateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserDataPermissionUpdateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserRoleUpdateRequest;
import io.swzxsyh.manager.api.dto.ManagerUserDtos.UserSummaryResponse;
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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端人员维护服务，复用 manager_admin_user 作为人员主表。 */
@Service
public class ManagerUserApplicationService extends ManagerApplicationSupport {

  private final ManagerAdminUserMapper adminUserMapper;
  private final ManagerRoleMapper roleMapper;
  private final ManagerUserRoleMapper userRoleMapper;
  private final ManagerRoleFunctionPermissionMapper functionPermissionMapper;
  private final ManagerRoleDataPermissionMapper dataPermissionMapper;
  private final ManagerUserDataPermissionMapper userDataPermissionMapper;
  private final PasswordEncoder passwordEncoder;

  public ManagerUserApplicationService(
      ManagerAdminUserMapper adminUserMapper,
      ManagerRoleMapper roleMapper,
      ManagerUserRoleMapper userRoleMapper,
      ManagerRoleFunctionPermissionMapper functionPermissionMapper,
      ManagerRoleDataPermissionMapper dataPermissionMapper,
      ManagerUserDataPermissionMapper userDataPermissionMapper,
      PasswordEncoder passwordEncoder) {
    this.adminUserMapper = adminUserMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.functionPermissionMapper = functionPermissionMapper;
    this.dataPermissionMapper = dataPermissionMapper;
    this.userDataPermissionMapper = userDataPermissionMapper;
    this.passwordEncoder = passwordEncoder;
  }

  /** 分页查询管理端人员，响应中不会返回密码摘要。 */
  public ManagerPageResponse<UserSummaryResponse> pageUsers(
      long page, long size, String username, Boolean enabled) {
    Page<ManagerAdminUser> result = adminUserMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<ManagerAdminUser>lambdaQuery()
            .like(hasText(username), ManagerAdminUser::getUsername, username)
            .eq(enabled != null, ManagerAdminUser::getEnabled, enabled)
            .orderByDesc(ManagerAdminUser::getCreatedAt));
    List<UserSummaryResponse> records = result.getRecords().stream()
        .map(user -> ManagerUserDtos.summary(user, roleCodes(user.getId())))
        .toList();
    return new ManagerPageResponse<>(result.getCurrent(), result.getSize(), result.getTotal(), records);
  }

  /** 查询人员详情，聚合已绑定角色及其有效功能/数据权限。 */
  public UserDetailResponse userDetail(Long userId) {
    ManagerAdminUser user = require(adminUserMapper.selectById(userId), "manager user not found");
    List<String> roles = roleCodes(user.getId());
    return new UserDetailResponse(
        ManagerUserDtos.summary(user, roleCodes(user.getId())),
        roleOptions(),
        effectiveFunctionPermissions(roles),
        userDataPermissions(user.getId()),
        effectiveDataPermissions(user.getId(), roles));
  }

  /** 返回可分配角色选项，供前端创建用户时勾选。 */
  public List<RoleOption> roleOptions() {
    return roleMapper.selectList(Wrappers.<ManagerRole>lambdaQuery()
            .eq(ManagerRole::getEnabled, Boolean.TRUE)
            .orderByAsc(ManagerRole::getRoleCode))
        .stream()
        .map(role -> new RoleOption(role.getRoleCode(), role.getRoleName(), role.getSuperAdmin()))
        .toList();
  }

  /** 三步向导最终创建人员，账号和角色绑定在同一事务内提交。 */
  @Transactional(rollbackFor = Exception.class)
  public UserDetailResponse createUser(UserCreateRequest request) {
    if (request == null || !hasText(request.username()) || !hasText(request.password())) {
      throw new IllegalArgumentException("username and password are required");
    }
    String username = request.username().trim();
    assertUsernameAvailable(username);
    LocalDateTime now = LocalDateTime.now();
    ManagerAdminUser user = new ManagerAdminUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setDisplayName(hasText(request.displayName()) ? request.displayName().trim() : username);
    requireContactPair(request.contactType(), request.contactValue());
    user.setContactType(normalizeContactType(request.contactType()));
    user.setContactValue(trimToNull(request.contactValue()));
    user.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    user.setAccountNonLocked(request.accountNonLocked() == null || Boolean.TRUE.equals(request.accountNonLocked()));
    user.setFailedLoginCount(0);
    user.setRoles("");
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    adminUserMapper.insert(user);

    replaceUserRoles(user.getId(), request.roleCodes(), now);
    replaceUserDataPermissions(user.getId(), request.dataPermissions(), now);
    return userDetail(user.getId());
  }

  /** 单独更新人员基础信息；password 为空时不修改密码。 */
  @Transactional(rollbackFor = Exception.class)
  public UserDetailResponse updateBasic(Long userId, UserBasicUpdateRequest request) {
    ManagerAdminUser user = require(adminUserMapper.selectById(userId), "manager user not found");
    if (request == null) {
      return userDetail(userId);
    }
    if (hasText(request.displayName())) {
      user.setDisplayName(request.displayName().trim());
    }
    requireContactPair(request.contactType(), request.contactValue());
    if (request.contactType() != null) {
      user.setContactType(normalizeContactType(request.contactType()));
    }
    if (request.contactValue() != null) {
      user.setContactValue(trimToNull(request.contactValue()));
    }
    if (hasText(request.password())) {
      user.setPasswordHash(passwordEncoder.encode(request.password()));
    }
    if (request.enabled() != null) {
      user.setEnabled(request.enabled());
    }
    if (request.accountNonLocked() != null) {
      user.setAccountNonLocked(request.accountNonLocked());
    }
    user.setUpdatedAt(LocalDateTime.now());
    adminUserMapper.updateById(user);
    return userDetail(userId);
  }

  /** 单独覆盖人员角色，功能权限和数据权限由角色自动联动。 */
  @Transactional(rollbackFor = Exception.class)
  public UserDetailResponse updateRoles(Long userId, UserRoleUpdateRequest request) {
    ManagerAdminUser user = require(adminUserMapper.selectById(userId), "manager user not found");
    LocalDateTime now = LocalDateTime.now();
    replaceUserRoles(user.getId(), request == null ? List.of() : request.roleCodes(), now);
    return userDetail(userId);
  }

  /** 单独覆盖人员数据权限，当前主要用于绑定可访问商户。 */
  @Transactional(rollbackFor = Exception.class)
  public UserDetailResponse updateDataPermissions(Long userId, UserDataPermissionUpdateRequest request) {
    ManagerAdminUser user = require(adminUserMapper.selectById(userId), "manager user not found");
    replaceUserDataPermissions(user.getId(), request == null ? List.of() : request.dataPermissions(), LocalDateTime.now());
    return userDetail(userId);
  }

  private void assertUsernameAvailable(String username) {
    ManagerAdminUser existing = adminUserMapper.selectOne(Wrappers.<ManagerAdminUser>lambdaQuery()
        .eq(ManagerAdminUser::getUsername, username)
        .last("limit 1"));
    if (existing != null) {
      throw new IllegalArgumentException("username already exists");
    }
  }

  private String normalizeContactType(String contactType) {
    if (!hasText(contactType)) {
      return null;
    }
    String normalized = contactType.trim().toUpperCase();
    if (!"PHONE".equals(normalized) && !"EMAIL".equals(normalized)) {
      throw new IllegalArgumentException("contactType must be PHONE or EMAIL");
    }
    return normalized;
  }

  private void requireContactPair(String contactType, String contactValue) {
    if (hasText(contactValue) && !hasText(contactType)) {
      throw new IllegalArgumentException("contactType is required when contactValue is configured");
    }
  }

  private String trimToNull(String value) {
    return hasText(value) ? value.trim() : null;
  }

  private List<String> roleCodes(Long userId) {
    return userRoleMapper.selectList(Wrappers.<ManagerUserRole>lambdaQuery()
            .eq(ManagerUserRole::getUserId, userId)
            .orderByAsc(ManagerUserRole::getRoleCode))
        .stream()
        .map(ManagerUserRole::getRoleCode)
        .toList();
  }

  private void replaceUserRoles(Long userId, List<String> roleCodes, LocalDateTime now) {
    userRoleMapper.delete(Wrappers.<ManagerUserRole>lambdaQuery().eq(ManagerUserRole::getUserId, userId));
    for (String roleCode : normalizeRoleCodes(roleCodes)) {
      require(roleMapper.selectOne(Wrappers.<ManagerRole>lambdaQuery()
          .eq(ManagerRole::getRoleCode, roleCode)
          .eq(ManagerRole::getEnabled, Boolean.TRUE)
          .last("limit 1")), "manager role not found or disabled: " + roleCode);
      ManagerUserRole binding = new ManagerUserRole();
      binding.setUserId(userId);
      binding.setRoleCode(roleCode);
      binding.setCreatedAt(now);
      userRoleMapper.insert(binding);
    }
  }

  private List<String> normalizeRoleCodes(List<String> values) {
    Set<String> result = new LinkedHashSet<>();
    if (values != null) {
      for (String value : values) {
        if (hasText(value)) {
          result.add(value.trim().toUpperCase());
        }
      }
    }
    return result.stream().toList();
  }

  private List<String> effectiveFunctionPermissions(List<String> roleCodes) {
    Set<String> permissions = new LinkedHashSet<>();
    for (String roleCode : roleCodes) {
      functionPermissionMapper.selectList(Wrappers.<ManagerRoleFunctionPermission>lambdaQuery()
              .eq(ManagerRoleFunctionPermission::getRoleCode, roleCode)
              .eq(ManagerRoleFunctionPermission::getEnabled, Boolean.TRUE)
              .orderByAsc(ManagerRoleFunctionPermission::getPermissionCode))
          .forEach(permission -> {
            if (hasText(permission.getPermissionCode())) {
              permissions.add(permission.getPermissionCode().trim());
            }
          });
    }
    return permissions.stream().toList();
  }

  private List<DataScopeRequest> userDataPermissions(Long userId) {
    return userDataPermissionMapper.selectList(Wrappers.<ManagerUserDataPermission>lambdaQuery()
            .eq(ManagerUserDataPermission::getUserId, userId)
            .eq(ManagerUserDataPermission::getEnabled, Boolean.TRUE)
            .orderByAsc(ManagerUserDataPermission::getScopeType)
            .orderByAsc(ManagerUserDataPermission::getScopeValue))
        .stream()
        .map(permission -> new DataScopeRequest(
            permission.getScopeType() == null ? "" : permission.getScopeType().trim().toUpperCase(),
            permission.getScopeValue() == null ? "" : permission.getScopeValue().trim()))
        .filter(scope -> hasText(scope.scopeType()) && hasText(scope.scopeValue()))
        .toList();
  }

  private void replaceUserDataPermissions(Long userId, List<DataScopeRequest> dataPermissions, LocalDateTime now) {
    userDataPermissionMapper.delete(Wrappers.<ManagerUserDataPermission>lambdaQuery()
        .eq(ManagerUserDataPermission::getUserId, userId));
    for (DataScopeRequest scope : normalizeDataScopes(dataPermissions)) {
      ManagerUserDataPermission permission = new ManagerUserDataPermission();
      permission.setUserId(userId);
      permission.setScopeType(scope.scopeType());
      permission.setScopeValue(scope.scopeValue());
      permission.setEnabled(Boolean.TRUE);
      permission.setCreatedAt(now);
      permission.setUpdatedAt(now);
      userDataPermissionMapper.insert(permission);
    }
  }

  private List<DataScopeRequest> normalizeDataScopes(List<DataScopeRequest> values) {
    Set<String> seen = new LinkedHashSet<>();
    java.util.ArrayList<DataScopeRequest> result = new java.util.ArrayList<>();
    if (values == null) {
      return result;
    }
    for (DataScopeRequest value : values) {
      if (value == null || !hasText(value.scopeType()) || !hasText(value.scopeValue())) {
        continue;
      }
      String scopeType = value.scopeType().trim().toUpperCase();
      String scopeValue = "ALL".equals(scopeType) ? "*" : value.scopeValue().trim();
      String key = scopeType + ":" + scopeValue;
      if (seen.add(key)) {
        result.add(new DataScopeRequest(scopeType, scopeValue));
      }
    }
    return result;
  }

  private List<DataScopeRequest> effectiveDataPermissions(Long userId, List<String> roleCodes) {
    Map<String, DataScopeRequest> scopes = new LinkedHashMap<>();
    for (String roleCode : roleCodes) {
      dataPermissionMapper.selectList(Wrappers.<ManagerRoleDataPermission>lambdaQuery()
              .eq(ManagerRoleDataPermission::getRoleCode, roleCode)
              .eq(ManagerRoleDataPermission::getEnabled, Boolean.TRUE)
              .orderByAsc(ManagerRoleDataPermission::getScopeType)
              .orderByAsc(ManagerRoleDataPermission::getScopeValue))
          .forEach(permission -> {
            if (hasText(permission.getScopeType()) && hasText(permission.getScopeValue())) {
              String key = permission.getScopeType().trim().toUpperCase() + ":" + permission.getScopeValue().trim();
              scopes.putIfAbsent(key, new DataScopeRequest(
                  permission.getScopeType().trim().toUpperCase(),
                  permission.getScopeValue().trim()));
            }
          });
    }
    for (DataScopeRequest scope : userDataPermissions(userId)) {
      scopes.putIfAbsent(scope.scopeType() + ":" + scope.scopeValue(), scope);
    }
    return scopes.values().stream().toList();
  }
}
