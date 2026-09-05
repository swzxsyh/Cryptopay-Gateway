package io.swzxsyh.manager.security.service;

import io.swzxsyh.manager.security.ManagerSecurityProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 基于 Redis 维护管理端 JWT 有效状态。 */
@Service
public class ManagerJwtTokenStore {

  private final RedissonClient redissonClient;
  private final ManagerSecurityProperties properties;

  public ManagerJwtTokenStore(RedissonClient redissonClient, ManagerSecurityProperties properties) {
    this.redissonClient = redissonClient;
    this.properties = properties;
  }

  /** 保存已签发 token 的 jti，用于后续校验和退出登录即时失效。 */
  public void save(ManagerJwtService.IssuedToken token) {
    long ttlMillis = Duration.between(Instant.now(), token.expiresAt()).toMillis();
    if (ttlMillis <= 0) {
      return;
    }
    redissonClient
        .getBucket(key(token.jti()))
        .set(token.username(), ttlMillis, TimeUnit.MILLISECONDS);
  }

  /** 判断 token 的 jti 是否仍处于有效登录状态。 */
  public boolean isActive(String jti) {
    return StringUtils.hasText(jti) && redissonClient.getBucket(key(jti)).isExists();
  }

  /** 删除 token 的 jti，使当前 token 立即失效。 */
  public void revoke(String jti) {
    if (StringUtils.hasText(jti)) {
      redissonClient.getBucket(key(jti)).delete();
    }
  }

  private String key(String jti) {
    return properties.getJwt().getRedisKeyPrefix() + ":" + jti;
  }
}
