package io.swzxsyh.manager.security;

import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** 管理端登录主体，集中携带账号、角色、功能权限和数据权限。 */
public class ManagerPrincipal implements UserDetails {

  private final ManagerAdminUser user;
  private final Set<String> roleCodes;
  private final Set<String> functionPermissions;
  private final List<ManagerDataScope> dataScopes;
  private final Collection<? extends GrantedAuthority> authorities;

  public ManagerPrincipal(
      ManagerAdminUser user,
      Set<String> roleCodes,
      Set<String> functionPermissions,
      List<ManagerDataScope> dataScopes,
      Collection<? extends GrantedAuthority> authorities) {
    this.user = user;
    this.roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    this.functionPermissions =
        functionPermissions == null ? Set.of() : Set.copyOf(functionPermissions);
    this.dataScopes = dataScopes == null ? List.of() : List.copyOf(dataScopes);
    this.authorities = authorities == null ? List.of() : List.copyOf(authorities);
  }

  public Long userId() {
    return user.getId();
  }

  public String displayName() {
    return user.getDisplayName();
  }

  public Set<String> roleCodes() {
    return roleCodes;
  }

  public Set<String> functionPermissions() {
    return functionPermissions;
  }

  public List<ManagerDataScope> dataScopes() {
    return dataScopes;
  }

  public boolean admin() {
    return roleCodes.stream().anyMatch(ManagerAuthority.ADMIN_ROLE::equalsIgnoreCase)
        || authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ManagerAuthority.role(ManagerAuthority.ADMIN_ROLE)::equalsIgnoreCase);
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return user.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return user.getUsername();
  }

  @Override
  public boolean isAccountNonLocked() {
    return Boolean.TRUE.equals(user.getAccountNonLocked());
  }

  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(user.getEnabled());
  }
}
