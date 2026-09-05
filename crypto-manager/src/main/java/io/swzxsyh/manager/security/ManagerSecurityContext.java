package io.swzxsyh.manager.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 管理端请求上下文，业务层可在当前请求线程中读取登录人信息。 */
public final class ManagerSecurityContext {

  private static final ThreadLocal<ManagerPrincipal> CURRENT = new ThreadLocal<>();

  private ManagerSecurityContext() {}

  public static void set(ManagerPrincipal principal) {
    if (principal == null) {
      CURRENT.remove();
      return;
    }
    CURRENT.set(principal);
  }

  public static Optional<ManagerPrincipal> current() {
    ManagerPrincipal principal = CURRENT.get();
    if (principal != null) {
      return Optional.of(principal);
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof ManagerPrincipal managerPrincipal) {
      return Optional.of(managerPrincipal);
    }
    return Optional.empty();
  }

  public static ManagerPrincipal requireCurrent() {
    return current().orElseThrow(() -> new IllegalStateException("manager user context is required"));
  }

  public static boolean isAdmin() {
    return current().map(ManagerPrincipal::admin).orElse(false);
  }

  public static void clear() {
    CURRENT.remove();
  }
}
