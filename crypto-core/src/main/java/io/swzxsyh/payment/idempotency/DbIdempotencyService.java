package io.swzxsyh.payment.idempotency;

import io.swzxsyh.payment.mapper.IdempotencyRecordMapper;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.persistence.entity.IdempotencyRecord;
import io.swzxsyh.payment.util.JsonUtil;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 基于数据库的幂等执行服务。 */
@Slf4j
@Service
public class DbIdempotencyService {

  private static final long DEFAULT_TTL_DAYS = 7L;

  private final IdempotencyRecordMapper mapper;
  private final LockUtil lockUtil;
  private final CryptoPaymentProperties properties;

  public DbIdempotencyService(
      IdempotencyRecordMapper mapper,
      LockUtil lockUtil,
      CryptoPaymentProperties properties) {
    this.mapper = mapper;
    this.lockUtil = lockUtil;
    this.properties = properties;
  }

  /** 在默认锁前缀下执行一次。 */
  public <T> T executeOnce(String scope, String idempotencyKey, Class<T> resultType, Supplier<T> action) {
    return executeOnce(scope, idempotencyKey, resultType, action, RedisKeyNamespace.dbIdempotencyLockPrefix(properties));
  }

  /** 在默认锁前缀下强制幂等执行，幂等键为空时直接拒绝。 */
  public <T> T executeOnceRequired(
      String scope, String idempotencyKey, Class<T> resultType, Supplier<T> action) {
    if (!StringUtils.hasText(idempotencyKey)) {
      throw new IllegalArgumentException("idempotencyKey is required");
    }
    return executeOnce(scope, idempotencyKey, resultType, action);
  }

  /** 在指定锁前缀下执行一次。 */
  public <T> T executeOnce(
      String scope,
      String idempotencyKey,
      Class<T> resultType,
      Supplier<T> action,
      String lockPrefix) {
    if (!StringUtils.hasText(idempotencyKey)) {
      return action.get();
    }

    String normalizedScope = normalize(scope);
    String normalizedKey = normalize(idempotencyKey);
    String lockKey = lockPrefix + normalizedScope + ":" + normalizedKey;
    return lockUtil.withLock(lockKey, 2000, 8, () -> {
      IdempotencyRecord record = mapper.selectOne(
          new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IdempotencyRecord>()
              .eq(IdempotencyRecord::getScope, normalizedScope)
              .eq(IdempotencyRecord::getIdempotencyKey, normalizedKey)
      );
      LocalDateTime now = LocalDateTime.now();
      if (record != null
          && record.getExpiresAt() != null
          && record.getExpiresAt().isBefore(now)) {
        mapper.deleteById(record.getId());
        record = null;
      }
      if (record != null) {
        if (StringUtils.hasText(record.getResponseJson())) {
          return deserialize(record.getResponseJson(), resultType);
        }
        return waitForCompletedResult(normalizedScope, normalizedKey, resultType);
      }

      record = new IdempotencyRecord();
      record.setScope(normalizedScope);
      record.setIdempotencyKey(normalizedKey);
      record.setExpiresAt(now.plusDays(DEFAULT_TTL_DAYS));
      record.setCreatedAt(now);
      record.setUpdatedAt(now);
      try {
        mapper.insert(record);
      } catch (DuplicateKeyException ex) {
        return waitForCompletedResult(normalizedScope, normalizedKey, resultType);
      }

      try {
        T result = action.get();
        if (result == null) {
          mapper.deleteById(record.getId());
          return null;
        }

        record.setResponseJson(serialize(result));
        record.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(record);
        return result;
      } catch (RuntimeException | Error ex) {
        mapper.deleteById(record.getId());
        throw ex;
      }
    });
  }

  /** 定期清理过期幂等记录，避免请求快照永久堆积。 */
  @org.springframework.scheduling.annotation.Scheduled(cron = "0 20 3 * * ?")
  public void purgeExpiredRecords() {
    LocalDateTime now = LocalDateTime.now();
    int deleted =
        mapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IdempotencyRecord>()
                .isNotNull(IdempotencyRecord::getExpiresAt)
                .lt(IdempotencyRecord::getExpiresAt, now));
    if (deleted > 0) {
      log.info("清理过期幂等记录完成。deletedCount={}", deleted);
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().replace(' ', '_').toLowerCase();
  }

  private <T> T waitForCompletedResult(String scope, String idempotencyKey, Class<T> resultType) {
    long deadline = System.currentTimeMillis() + idempotencyWaitMillis();
    while (System.currentTimeMillis() < deadline) {
      Optional<IdempotencyRecord> current = findRecord(scope, idempotencyKey);
      if (current.isPresent() && StringUtils.hasText(current.get().getResponseJson())) {
        return deserialize(current.get().getResponseJson(), resultType);
      }
      sleepQuietly(250L);
    }
    throw new IdempotentRequestInProgressException();
  }

  private long idempotencyWaitMillis() {
    return Math.max(1000L, properties.getIdempotencyWaitMillis());
  }

  private Optional<IdempotencyRecord> findRecord(String scope, String idempotencyKey) {
    return Optional.ofNullable(
        mapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IdempotencyRecord>()
                .eq(IdempotencyRecord::getScope, scope)
                .eq(IdempotencyRecord::getIdempotencyKey, idempotencyKey)));
  }

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for idempotent result", ex);
    }
  }

  private String serialize(Object value) {
    return JsonUtil.toJson(value);
  }

  private <T> T deserialize(String json, Class<T> type) {
    return JsonUtil.fromJson(json, type);
  }
}
