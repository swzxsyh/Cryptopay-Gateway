package io.swzxsyh.manager.security;

import org.springframework.util.StringUtils;

/** 管理端 Spring Security authority 编码工具。 */
public final class ManagerAuthority {

  public static final String ROLE_PREFIX = "ROLE_";
  public static final String PERMISSION_PREFIX = "PERM_";
  public static final String ADMIN_ROLE = "ADMIN";

  private ManagerAuthority() {}

  public static String role(String roleCode) {
    return ROLE_PREFIX + normalize(roleCode);
  }

  public static String permission(String permissionCode) {
    return PERMISSION_PREFIX + normalize(permissionCode);
  }

  public static String normalize(String value) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase() : "";
  }
}
