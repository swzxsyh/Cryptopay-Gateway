package io.swzxsyh.payment.idempotency;

import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.JsonUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 基于 Redis 的幂等执行服务。 */
@Slf4j
@Service
@ConditionalOnBean(RedisUtil.class)
public class RedisIdempotencyService {

  private static final Duration DEFAULT_TTL = Duration.ofDays(2);
  private static final String IN_PROGRESS = "__IN_PROGRESS__";

  private final RedisUtil redisUtil;
  private final LockUtil lockUtil;
  private final CryptoPaymentProperties properties;

  public RedisIdempotencyService(
      RedisUtil redisUtil,
      LockUtil lockUtil,
      CryptoPaymentProperties properties) {
    this.redisUtil = redisUtil;
    this.lockUtil = lockUtil;
    this.properties = properties;
  }

  /** 在默认 TTL 下执行一次。 */
  public <T> T executeOnce(String scope, String idempotencyKey, Class<T> resultType, Supplier<T> action) {
    return executeOnce(scope, idempotencyKey, resultType, DEFAULT_TTL, action);
  }

  /** 在默认 TTL 下强制幂等执行，幂等键为空时直接拒绝。 */
  public <T> T executeOnceRequired(
      String scope, String idempotencyKey, Class<T> resultType, Supplier<T> action) {
    if (!StringUtils.hasText(idempotencyKey)) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }
    return executeOnce(scope, idempotencyKey, resultType, action);
  }

  /** 在指定 TTL 下执行一次。 */
  public <T> T executeOnce(
      String scope,
      String idempotencyKey,
      Class<T> resultType,
      Duration ttl,
      Supplier<T> action) {
    if (!StringUtils.hasText(idempotencyKey)) {
      return action.get();
    }

    String normalizedScope = normalize(scope);
    String normalizedKey = normalize(idempotencyKey);
    String bucketKey = RedisKeyNamespace.idempotencyBucket(properties, normalizedScope, normalizedKey);
    String lockKey = bucketKey + ":lock";

    return lockUtil.withLock(lockKey, 2000, 8, () -> {
      String cached = redisUtil.getString(bucketKey);
      if (StringUtils.hasText(cached)) {
        if (IN_PROGRESS.equals(cached)) {
          return waitForCompletedResult(bucketKey, resultType);
        }
        return deserialize(cached, resultType);
      }

      redisUtil.setString(bucketKey, IN_PROGRESS, pendingTtlMillis(ttl), TimeUnit.MILLISECONDS);
      try {
        T result = action.get();
        if (result == null) {
          redisUtil.deleteStringIfEquals(bucketKey, IN_PROGRESS);
          return null;
        }

        redisUtil.setString(bucketKey, serialize(result), ttl.toMillis(), TimeUnit.MILLISECONDS);
        return result;
      } catch (RuntimeException | Error ex) {
        redisUtil.deleteStringIfEquals(bucketKey, IN_PROGRESS);
        throw ex;
      }
    });
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().replace(' ', '_').toLowerCase();
  }

  private long pendingTtlMillis(Duration ttl) {
    long requested = ttl == null ? DEFAULT_TTL.toMillis() : ttl.toMillis();
    return Math.max(1000L, Math.min(Duration.ofMinutes(2).toMillis(), requested));
  }

  private String serialize(Object value) {
    return JsonUtil.toJson(value);
  }

  private <T> T waitForCompletedResult(String bucketKey, Class<T> resultType) {
    long deadline = System.currentTimeMillis() + idempotencyWaitMillis();
    while (System.currentTimeMillis() < deadline) {
      sleepQuietly(250L);
      String cached = redisUtil.getString(bucketKey);
      if (StringUtils.hasText(cached) && !IN_PROGRESS.equals(cached)) {
        return deserialize(cached, resultType);
      }
    }
    throw new IdempotentRequestInProgressException();
  }

  private long idempotencyWaitMillis() {
    return Math.max(1000L, properties.getIdempotencyWaitMillis());
  }

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for idempotent result", ex);
    }
  }

  private <T> T deserialize(String json, Class<T> type) {
    return JsonUtil.fromJson(json, type);
  }
}
