package io.swzxsyh.manager.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.manager.security.ManagerSecurityProperties;
import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import io.swzxsyh.manager.security.entity.ManagerUserRole;
import io.swzxsyh.manager.security.mapper.ManagerAdminUserMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserRoleMapper;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 启动时引导创建管理端初始管理员。 */
@Component
@Order(1)
public class ManagerAdminBootstrapService implements ApplicationRunner {

  private final ManagerSecurityProperties properties;
  private final ManagerAdminUserMapper adminUserMapper;
  private final ManagerUserRoleMapper userRoleMapper;
  private final PasswordEncoder passwordEncoder;

  public ManagerAdminBootstrapService(
      ManagerSecurityProperties properties,
      ManagerAdminUserMapper adminUserMapper,
      ManagerUserRoleMapper userRoleMapper,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.adminUserMapper = adminUserMapper;
    this.userRoleMapper = userRoleMapper;
    this.passwordEncoder = passwordEncoder;
  }

  /** 根据配置创建或更新 bootstrap 管理员，数据库中始终只保存密码摘要。 */
  @Override
  public void run(ApplicationArguments args) {
    ManagerSecurityProperties.BootstrapAdmin admin = properties.getBootstrapAdmin();
    if (!admin.isEnabled()) {
      return;
    }
    if (!StringUtils.hasText(admin.getUsername())) {
      throw new IllegalStateException("manager.security.bootstrap-admin.username must be configured");
    }
    String passwordHash = resolvePasswordHash(admin);
    if (!StringUtils.hasText(passwordHash)) {
      throw new IllegalStateException(
          "manager.security.bootstrap-admin.password or password-hash must be configured");
    }

    ManagerAdminUser existing =
        adminUserMapper.selectOne(
            new LambdaQueryWrapper<ManagerAdminUser>()
                .eq(ManagerAdminUser::getUsername, admin.getUsername())
                .last("limit 1"));
    LocalDateTime now = LocalDateTime.now();
    String roles = String.join(",", admin.getRoles());

    if (existing == null) {
      ManagerAdminUser user = new ManagerAdminUser();
      user.setUsername(admin.getUsername());
      user.setPasswordHash(passwordHash);
      user.setDisplayName(admin.getUsername());
      user.setEnabled(true);
      user.setAccountNonLocked(true);
      user.setFailedLoginCount(0);
      user.setRoles(roles);
      user.setCreatedAt(now);
      user.setUpdatedAt(now);
      adminUserMapper.insert(user);
      bindRoles(user.getId(), admin.getRoles(), now);
      return;
    }

    boolean shouldUpdatePassword = admin.isResetPasswordOnStartup();
    existing.setRoles(roles);
    existing.setEnabled(true);
    existing.setAccountNonLocked(true);
    existing.setUpdatedAt(now);
    if (shouldUpdatePassword) {
      existing.setPasswordHash(passwordHash);
    }
    adminUserMapper.updateById(existing);
    bindRoles(existing.getId(), admin.getRoles(), now);
  }

  private void bindRoles(Long userId, java.util.List<String> roles, LocalDateTime now) {
    if (userId == null || roles == null || roles.isEmpty()) {
      return;
    }
    for (String role : roles) {
      if (!StringUtils.hasText(role)) {
        continue;
      }
      String roleCode = role.trim().toUpperCase();
      ManagerUserRole existing =
          userRoleMapper.selectOne(
              new LambdaQueryWrapper<ManagerUserRole>()
                  .eq(ManagerUserRole::getUserId, userId)
                  .eq(ManagerUserRole::getRoleCode, roleCode)
                  .last("limit 1"));
      if (existing != null) {
        continue;
      }
      ManagerUserRole binding = new ManagerUserRole();
      binding.setUserId(userId);
      binding.setRoleCode(roleCode);
      binding.setCreatedAt(now);
      userRoleMapper.insert(binding);
    }
  }

  private String resolvePasswordHash(ManagerSecurityProperties.BootstrapAdmin admin) {
    if (StringUtils.hasText(admin.getPasswordHash())) {
      return admin.getPasswordHash();
    }
    if (StringUtils.hasText(admin.getPassword())) {
      return passwordEncoder.encode(admin.getPassword());
    }
    return null;
  }
}
