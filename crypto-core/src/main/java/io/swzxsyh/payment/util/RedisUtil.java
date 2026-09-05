package io.swzxsyh.payment.util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RDeque;
import org.redisson.api.RMapCache;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Redis 常用操作封装。 */
@Slf4j
@Component
@ConditionalOnBean(RedissonClient.class)
public class RedisUtil {

  private final RedissonClient redissonClient;

  public RedisUtil(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  public <T> RDeque<T> deque(String key) {
    return redissonClient.getDeque(key);
  }

  public <K, V> RMapCache<K, V> mapCache(String key) {
    return redissonClient.getMapCache(key);
  }

  public RSet<String> set(String key) {
    return redissonClient.getSet(key);
  }

  public String pollFirst(String dequeKey) {
    return asStringDeque(dequeKey).pollFirst();
  }

  public void addLastIfAbsent(String dequeKey, String value) {
    if (!StringUtils.hasText(value)) {
      return;
    }
    RDeque<String> deque = asStringDeque(dequeKey);
    if (!deque.contains(value)) {
      deque.addLast(value);
    }
  }

  public boolean removeValue(String dequeKey, String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    return asStringDeque(dequeKey).remove(value);
  }

  public List<String> readAllStrings(String dequeKey) {
    return asStringDeque(dequeKey).readAll().stream().map(Object::toString).toList();
  }

  public Set<String> readStringSet(String setKey) {
    return Set.copyOf(set(setKey).readAll());
  }

  public boolean addSetValue(String setKey, String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    return set(setKey).add(value);
  }

  public boolean removeSetValue(String setKey, String value) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    return set(setKey).remove(value);
  }

  public long mapSize(String mapKey) {
    return redissonClient.getMapCache(mapKey).size();
  }

  public <V> void putWithLease(String mapKey, String entryKey, V value, long ttl, TimeUnit unit) {
    redissonClient.<String, V>getMapCache(mapKey).put(entryKey, value, ttl, unit);
  }

  public <V> V remove(String mapKey, String entryKey) {
    return redissonClient.<String, V>getMapCache(mapKey).remove(entryKey);
  }

  public String getString(String key) {
    return redissonClient.<String>getBucket(key).get();
  }

  public void setString(String key, String value, long ttl, TimeUnit unit) {
    redissonClient.<String>getBucket(key).set(value, ttl, unit);
  }

  public boolean trySetString(String key, String value, long ttl, TimeUnit unit) {
    RBucket<String> bucket = redissonClient.getBucket(key);
    return bucket.trySet(value, ttl, unit);
  }

  public boolean renewStringIfEquals(String key, String expectedValue, long ttl, TimeUnit unit) {
    if (!StringUtils.hasText(expectedValue)) {
      return false;
    }
    RBucket<String> bucket = redissonClient.getBucket(key);
    String current = bucket.get();
    if (!expectedValue.equals(current)) {
      return false;
    }
    bucket.set(expectedValue, ttl, unit);
    return true;
  }

  public boolean deleteStringIfEquals(String key, String expectedValue) {
    if (!StringUtils.hasText(expectedValue)) {
      return false;
    }
    RBucket<String> bucket = redissonClient.getBucket(key);
    String current = bucket.get();
    if (!expectedValue.equals(current)) {
      return false;
    }
    return bucket.delete();
  }

  public long incrementAndExpire(String key, long ttl, TimeUnit unit) {
    RAtomicLong counter = redissonClient.getAtomicLong(key);
    long value = counter.incrementAndGet();
    if (value == 1L) {
      counter.expire(ttl, unit);
    }
    return value;
  }

  private RDeque<String> asStringDeque(String key) {
    return redissonClient.getDeque(key);
  }
}
