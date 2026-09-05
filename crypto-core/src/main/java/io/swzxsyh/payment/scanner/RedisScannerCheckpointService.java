package io.swzxsyh.payment.scanner;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.persistence.entity.ChainScannerCheckpoint;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Redis 扫描 checkpoint 服务，把热点进度放在 Redis，按批次回写数据库。 */
@Slf4j
@Service
public class RedisScannerCheckpointService {

  private final RedisUtil redisUtil;
  private final ChainScannerCheckpointService checkpointService;
  private final CryptoPaymentProperties properties;

  public RedisScannerCheckpointService(
      RedisUtil redisUtil,
      ChainScannerCheckpointService checkpointService,
      CryptoPaymentProperties properties) {
    this.redisUtil = redisUtil;
    this.checkpointService = checkpointService;
    this.properties = properties;
  }

  /** 从数据库同步指定链的 checkpoint 到 Redis。 */
  public void bootstrap(String chain) {
    if (!StringUtils.hasText(chain)) {
      return;
    }
    ChainScannerCheckpoint checkpoint = checkpointService.getOrCreate(chain);
    String key = redisKey(chain);
    redisUtil.mapCache(key).put("latestObservedBlock", String.valueOf(checkpoint.getLatestObservedBlock()));
    redisUtil.mapCache(key).put("lastConfirmedBlock", String.valueOf(checkpoint.getLastConfirmedBlock()));
    redisUtil.mapCache(key).put("lastFlushedConfirmedBlock", String.valueOf(checkpoint.getLastConfirmedBlock()));
    redisUtil.mapCache(key).put("updatedAt", String.valueOf(LocalDateTime.now()));
    log.info("已从数据库同步扫描 checkpoint 到 Redis。chain={}, latestObservedBlock={}, lastConfirmedBlock={}, redisKey={}",
        chain,
        checkpoint.getLatestObservedBlock(),
        checkpoint.getLastConfirmedBlock(),
        key);
  }

  /** 仅更新 Redis 中的最新观察块高度。 */
  public long updateObservedBlock(String chain, long latestObservedBlock) {
    return updateValue(chain, "latestObservedBlock", latestObservedBlock);
  }

  /** 仅更新 Redis 中的最新确认块高度。 */
  public long updateConfirmedBlock(String chain, long lastConfirmedBlock) {
    return updateValue(chain, "lastConfirmedBlock", lastConfirmedBlock);
  }

  /** 获取最后确认块，高优先级读取 Redis。 */
  public long getLastConfirmedBlock(String chain) {
    String value = stringValue(chain, "lastConfirmedBlock");
    if (StringUtils.hasText(value)) {
      return parseLong(value, 0L);
    }
    return checkpointService.getOrCreate(chain).getLastConfirmedBlock();
  }

  /** 获取最后观察块，高优先级读取 Redis。 */
  public long getLatestObservedBlock(String chain) {
    String value = stringValue(chain, "latestObservedBlock");
    if (StringUtils.hasText(value)) {
      return parseLong(value, 0L);
    }
    return checkpointService.getOrCreate(chain).getLatestObservedBlock();
  }

  /** 按批次阈值把 Redis checkpoint 落回数据库。 */
  public void flushIfNeeded(String chain, boolean force) {
    if (!StringUtils.hasText(chain)) {
      return;
    }
    long confirmed = getLastConfirmedBlock(chain);
    long flushed = parseLong(stringValue(chain, "lastFlushedConfirmedBlock"), 0L);
    int batchSize = Math.max(1, properties.getScanner().getCheckpointFlushBlocks());
    if (!force && confirmed - flushed < batchSize) {
      log.debug("暂不回写扫描 checkpoint。chain={}, confirmedBlock={}, flushedBlock={}, batchSize={}, force={}",
          chain, confirmed, flushed, batchSize, force);
      return;
    }

    ChainScannerCheckpoint checkpoint = checkpointService.getOrCreate(chain);
    long observed = getLatestObservedBlock(chain);
    if (observed > checkpoint.getLatestObservedBlock()) {
      checkpointService.updateObservedBlock(chain, observed);
    }
    if (confirmed > checkpoint.getLastConfirmedBlock()) {
      checkpointService.updateConfirmedBlock(chain, confirmed);
    }
    redisUtil.mapCache(redisKey(chain)).put("lastFlushedConfirmedBlock", String.valueOf(confirmed));
    redisUtil.mapCache(redisKey(chain)).put("updatedAt", String.valueOf(LocalDateTime.now()));
    log.info("扫描 checkpoint 已回写数据库。chain={}, confirmedBlock={}, batchSize={}, force={}",
        chain, confirmed, batchSize, force);
  }

  private long updateValue(String chain, String field, long value) {
    if (!StringUtils.hasText(chain)) {
      return value;
    }
    String key = redisKey(chain);
    long current = parseLong(stringValue(chain, field), 0L);
    if (value > current) {
      redisUtil.mapCache(key).put(field, String.valueOf(value));
      redisUtil.mapCache(key).put("updatedAt", String.valueOf(LocalDateTime.now()));
      return value;
    }
    return current;
  }

  private String stringValue(String chain, String field) {
    if (!StringUtils.hasText(chain)) {
      return null;
    }
    Object value = redisUtil.mapCache(redisKey(chain)).get(field);
    return value == null ? null : Objects.toString(value, null);
  }

  private long parseLong(String value, long defaultValue) {
    if (!StringUtils.hasText(value)) {
      return defaultValue;
    }
    try {
      return Long.parseLong(value);
    } catch (Exception ex) {
      return defaultValue;
    }
  }

  private String redisKey(String chain) {
    return RedisKeyNamespace.scannerCheckpoint(properties, chain);
  }
}
