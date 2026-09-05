package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerApiResponseCode;
import io.swzxsyh.manager.api.dto.ManagerLoginRequest;
import io.swzxsyh.manager.api.dto.ManagerLoginResponse;
import io.swzxsyh.manager.security.ManagerPrincipal;
import io.swzxsyh.manager.security.service.ManagerJwtService;
import io.swzxsyh.manager.security.service.ManagerJwtTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/auth")
public class ManagerAuthController {

  private final AuthenticationManager authenticationManager;
  private final ManagerJwtService jwtService;
  private final ManagerJwtTokenStore tokenStore;

  public ManagerAuthController(
      AuthenticationManager authenticationManager,
      ManagerJwtService jwtService,
      ManagerJwtTokenStore tokenStore) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.tokenStore = tokenStore;
  }

  /** 管理端登录，认证成功后签发 JWT 并写入 Redis 有效 token 列表。 */
  @PostMapping("/login")
  public ResponseEntity<ManagerApiResponse<?>> login(@RequestBody ManagerLoginRequest request) {
    if (request == null
        || request.username() == null
        || request.username().isBlank()
        || request.password() == null
        || request.password().isBlank()) {
      return ResponseEntity.badRequest()
          .body(
              ManagerApiResponse.fail(
                  ManagerApiResponseCode.BAD_REQUEST, "Username and password required"));
    }

    try {
      Authentication authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  request.username(), request.password()));
      ManagerJwtService.IssuedToken token = jwtService.issue(authentication);
      tokenStore.save(token);
      return ResponseEntity.ok(ManagerApiResponse.ok(toResponse(authentication, token)));
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              ManagerApiResponse.fail(
                  ManagerApiResponseCode.UNAUTHORIZED, "Invalid username or password"));
    }
  }

  /** 管理端退出登录，从 Redis 移除当前 JWT，使其立即失效。 */
  @PostMapping("/logout")
  public ManagerApiResponse<Void> logout(HttpServletRequest request) {
    String token = resolveBearerToken(request);
    if (token != null) {
      ManagerJwtService.VerifiedToken verified = jwtService.verify(token);
      tokenStore.revoke(verified.jti());
    }
    SecurityContextHolder.clearContext();
    return ManagerApiResponse.ok(null);
  }

  /** 查询当前登录管理员信息。 */
  @GetMapping("/me")
  public ManagerApiResponse<ManagerLoginResponse> me(
      @AuthenticationPrincipal UserDetails userDetails) {
    return ManagerApiResponse.ok(
        new ManagerLoginResponse(
            userDetails.getUsername(),
            userDetails.getAuthorities().stream().map(Object::toString).toList(),
            roles(userDetails),
            functionPermissions(userDetails),
            dataScopes(userDetails),
            null,
            null,
            null));
  }

  private ManagerLoginResponse toResponse(
      Authentication authentication, ManagerJwtService.IssuedToken token) {
    List<String> authorities =
        authentication.getAuthorities().stream().map(Object::toString).toList();
    return new ManagerLoginResponse(
        authentication.getName(),
        authorities,
        roles(authentication.getPrincipal()),
        functionPermissions(authentication.getPrincipal()),
        dataScopes(authentication.getPrincipal()),
        "Bearer",
        token.token(),
        token.expiresAt());
  }

  private List<String> roles(Object principal) {
    if (principal instanceof ManagerPrincipal managerPrincipal) {
      return managerPrincipal.roleCodes().stream().toList();
    }
    return List.of();
  }

  private List<String> functionPermissions(Object principal) {
    if (principal instanceof ManagerPrincipal managerPrincipal) {
      return managerPrincipal.functionPermissions().stream().toList();
    }
    return List.of();
  }

  private List<?> dataScopes(Object principal) {
    if (principal instanceof ManagerPrincipal managerPrincipal) {
      return managerPrincipal.dataScopes();
    }
    return List.of();
  }

  private String resolveBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      return null;
    }
    return header.substring(7).trim();
  }
}
