package io.swzxsyh.manager.security.service;

import io.swzxsyh.manager.security.ManagerSecurityProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 管理端 JWT 签发与校验服务。 */
@Service
public class ManagerJwtService {

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  private final ManagerSecurityProperties properties;
  private final ObjectMapper objectMapper;
  private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
  private final Base64.Decoder urlDecoder = Base64.getUrlDecoder();

  public ManagerJwtService(ManagerSecurityProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  /** 启动时校验 JWT 配置，避免使用空密钥或过短密钥运行管理端。 */
  @PostConstruct
  public void validateConfig() {
    String secret = properties.getJwt().getSecret();
    if (!StringUtils.hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("manager.security.jwt.secret must contain at least 32 bytes");
    }
    if (properties.getJwt().getAccessTokenTtlSeconds() <= 0) {
      throw new IllegalStateException("manager.security.jwt.access-token-ttl-seconds must be positive");
    }
  }

  /** 为认证成功的管理员签发访问令牌。 */
  public IssuedToken issue(Authentication authentication) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(properties.getJwt().getAccessTokenTtlSeconds());
    String jti = UUID.randomUUID().toString().replace("-", "");
    List<String> authorities =
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("iss", properties.getJwt().getIssuer());
    payload.put("sub", authentication.getName());
    payload.put("jti", jti);
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", expiresAt.getEpochSecond());
    payload.put("authorities", authorities);

    String unsigned = encodeJson(header) + "." + encodeJson(payload);
    String token = unsigned + "." + sign(unsigned);
    return new IssuedToken(token, jti, authentication.getName(), authorities, expiresAt);
  }

  /** 解析并校验 JWT 签名、签发方和过期时间。 */
  public VerifiedToken verify(String token) {
    if (!StringUtils.hasText(token)) {
      throw new IllegalArgumentException("Token required");
    }
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid token format");
    }
    String unsigned = parts[0] + "." + parts[1];
    if (!constantTimeEquals(sign(unsigned), parts[2])) {
      throw new IllegalArgumentException("Invalid token signature");
    }

    Map<String, Object> payload = decodeJson(parts[1]);
    if (!properties.getJwt().getIssuer().equals(String.valueOf(payload.get("iss")))) {
      throw new IllegalArgumentException("Invalid token issuer");
    }
    long expiresAt = asLong(payload.get("exp"));
    if (Instant.now().getEpochSecond() >= expiresAt) {
      throw new IllegalArgumentException("Token expired");
    }

    String username = String.valueOf(payload.get("sub"));
    String jti = String.valueOf(payload.get("jti"));
    if (!StringUtils.hasText(username) || !StringUtils.hasText(jti)) {
      throw new IllegalArgumentException("Invalid token claims");
    }
    return new VerifiedToken(jti, username, Instant.ofEpochSecond(expiresAt));
  }

  private String encodeJson(Map<String, Object> value) {
    try {
      return urlEncoder.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (Exception e) {
      throw new IllegalStateException("JWT encode failed", e);
    }
  }

  private Map<String, Object> decodeJson(String value) {
    try {
      return objectMapper.readValue(urlDecoder.decode(value), new TypeReference<>() {});
    } catch (Exception e) {
      throw new IllegalArgumentException("JWT decode failed", e);
    }
  }

  private String sign(String unsignedToken) {
    String secret = properties.getJwt().getSecret();
    try {
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
      return urlEncoder.encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("JWT sign failed", e);
    }
  }

  private boolean constantTimeEquals(String expected, String actual) {
    byte[] a = expected.getBytes(StandardCharsets.UTF_8);
    byte[] b = actual.getBytes(StandardCharsets.UTF_8);
    if (a.length != b.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < a.length; i++) {
      result |= a[i] ^ b[i];
    }
    return result == 0;
  }

  private long asLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  public record IssuedToken(
      String token,
      String jti,
      String username,
      Collection<String> authorities,
      Instant expiresAt) {}

  public record VerifiedToken(String jti, String username, Instant expiresAt) {}
}
