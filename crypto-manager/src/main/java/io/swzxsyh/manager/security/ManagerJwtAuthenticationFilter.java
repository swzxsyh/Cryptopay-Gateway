package io.swzxsyh.manager.security;

import io.swzxsyh.manager.security.service.ManagerJwtService;
import io.swzxsyh.manager.security.service.ManagerJwtTokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/** 解析 Authorization Bearer JWT 并写入 Spring Security 上下文。 */
@Component
public class ManagerJwtAuthenticationFilter extends OncePerRequestFilter {

  private final ManagerJwtService jwtService;
  private final ManagerJwtTokenStore tokenStore;
  private final UserDetailsService userDetailsService;

  public ManagerJwtAuthenticationFilter(
      ManagerJwtService jwtService,
      ManagerJwtTokenStore tokenStore,
      UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.tokenStore = tokenStore;
    this.userDetailsService = userDetailsService;
  }

  /** 每个请求尝试解析 Bearer token，校验通过后加载数据库用户信息。 */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String token = resolveBearerToken(request);
      if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
        try {
          ManagerJwtService.VerifiedToken verified = jwtService.verify(token);
          if (tokenStore.isActive(verified.jti())) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(verified.username());
            UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            if (userDetails instanceof ManagerPrincipal principal) {
              ManagerSecurityContext.set(principal);
            }
          }
        } catch (RuntimeException ignored) {
          SecurityContextHolder.clearContext();
        }
      }
      filterChain.doFilter(request, response);
    } finally {
      ManagerSecurityContext.clear();
    }
  }

  private String resolveBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
      return null;
    }
    return header.substring(7).trim();
  }
}
