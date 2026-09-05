package io.swzxsyh.manager.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** 管理端雪花 ID 工具，用于生成商户号等后台主数据编号。 */
@Component
public class SnowflakeIdUtil {

  private static final long CUSTOM_EPOCH_MILLIS = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
  private static final long NODE_BITS = 10L;
  private static final long SEQUENCE_BITS = 12L;
  private static final long MAX_NODE_ID = (1L << NODE_BITS) - 1L;
  private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1L;
  private static final long NODE_SHIFT = SEQUENCE_BITS;
  private static final long TIMESTAMP_SHIFT = NODE_BITS + SEQUENCE_BITS;

  private final long nodeId;
  private long lastTimestamp = -1L;
  private long sequence = 0L;

  public SnowflakeIdUtil() {
    this.nodeId = resolveNodeId();
  }

  /** 生成数字型雪花 ID。 */
  public synchronized long nextId() {
    long timestamp = currentTimestamp();
    if (timestamp < lastTimestamp) {
      timestamp = waitUntil(lastTimestamp);
    }
    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1L) & MAX_SEQUENCE;
      if (sequence == 0L) {
        timestamp = waitUntil(lastTimestamp + 1L);
      }
    } else {
      sequence = 0L;
    }
    lastTimestamp = timestamp;
    return ((timestamp - CUSTOM_EPOCH_MILLIS) << TIMESTAMP_SHIFT)
        | (nodeId << NODE_SHIFT)
        | sequence;
  }

  /** 生成商户号，格式为 MCH + 雪花数字，长度小于 merchant_id varchar(64)。 */
  public String nextMerchantId() {
    return "MCH" + nextId();
  }

  private long currentTimestamp() {
    return System.currentTimeMillis();
  }

  private long waitUntil(long targetTimestamp) {
    long timestamp = currentTimestamp();
    while (timestamp < targetTimestamp) {
      Thread.onSpinWait();
      timestamp = currentTimestamp();
    }
    return timestamp;
  }

  private long resolveNodeId() {
    String configured = System.getenv("CRYPTO_PAYMENT_SNOWFLAKE_NODE_ID");
    if (configured != null && !configured.isBlank()) {
      try {
        return Long.parseLong(configured.trim()) & MAX_NODE_ID;
      } catch (NumberFormatException ignored) {
        // 回退到本机信息派生，避免配置错误导致服务无法启动。
      }
    }
    String source = ManagementFactory.getRuntimeMXBean().getName() + "-" + new SecureRandom().nextInt();
    try {
      source = InetAddress.getLocalHost().getHostName() + "-" + source;
    } catch (Exception ignored) {
      // 本机名不可用时使用运行时标识。
    }
    return Math.abs(source.hashCode()) & MAX_NODE_ID;
  }
}
