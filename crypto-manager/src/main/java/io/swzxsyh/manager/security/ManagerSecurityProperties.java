package io.swzxsyh.manager.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "manager.security")
public class ManagerSecurityProperties {

  private final BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

  private final Jwt jwt = new Jwt();

  public BootstrapAdmin getBootstrapAdmin() {
    return bootstrapAdmin;
  }

  public Jwt getJwt() {
    return jwt;
  }

  public static class BootstrapAdmin {

    private boolean enabled = true;

    private String username = "admin";

    private String password = "";

    private String passwordHash = "";

    private boolean resetPasswordOnStartup = false;

    private List<String> roles = List.of("ADMIN");

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPasswordHash() {
      return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
      this.passwordHash = passwordHash;
    }

    public boolean isResetPasswordOnStartup() {
      return resetPasswordOnStartup;
    }

    public void setResetPasswordOnStartup(boolean resetPasswordOnStartup) {
      this.resetPasswordOnStartup = resetPasswordOnStartup;
    }

    public List<String> getRoles() {
      return roles;
    }

    public void setRoles(List<String> roles) {
      this.roles = roles;
    }
  }

  public static class Jwt {

    private String issuer = "crypto-gateway-manager";

    private String secret = "";

    private long accessTokenTtlSeconds = 7200;

    private String redisKeyPrefix = "crypto:manager:jwt";

    public String getIssuer() {
      return issuer;
    }

    public void setIssuer(String issuer) {
      this.issuer = issuer;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public long getAccessTokenTtlSeconds() {
      return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
      this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public String getRedisKeyPrefix() {
      return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
      this.redisKeyPrefix = redisKeyPrefix;
    }
  }
}
