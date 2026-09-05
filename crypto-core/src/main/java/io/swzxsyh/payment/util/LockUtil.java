package io.swzxsyh.payment.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/** 分布式锁的统一封装。 */
@Slf4j
@Component
public class LockUtil {

  private final RedissonClient redissonClient;

  public LockUtil(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  public <T> T withLock(String lockKey, long waitMillis, long leaseSeconds, Callable<T> action) {
    RLock lock = redissonClient.getLock(lockKey);
    boolean locked = false;
    try {
      locked = lock.tryLock(waitMillis, leaseSeconds, TimeUnit.SECONDS);
      if (!locked) {
        throw new IllegalStateException("Failed to acquire lock: " + lockKey);
      }
      return action.call();
    } catch (Exception ex) {
      if (ex instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Lock execution failed: " + lockKey, ex);
    } finally {
      if (locked && lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  public void withLock(String lockKey, long waitMillis, long leaseSeconds, Runnable action) {
    withLock(
        lockKey,
        waitMillis,
        leaseSeconds,
        () -> {
          action.run();
          return null;
        });
  }
}
