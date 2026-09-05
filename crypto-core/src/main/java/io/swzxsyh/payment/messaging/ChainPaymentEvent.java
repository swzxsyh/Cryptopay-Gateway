package io.swzxsyh.payment.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** 链上入账事件，作为扫描器和支付业务处理之间的消息边界。 */
public record ChainPaymentEvent(
    String eventId,
    String source,
    String chain,
    String token,
    String tokenAddress,
    String txHash,
    String sourceAddress,
    String destinationAddress,
    BigDecimal amount,
    Long blockNumber,
    Long logIndex,
    Instant blockTimestamp,
    Instant observedAt) {

  /** 构建普通链上入账事件，并生成稳定的事件编号便于日志追踪。 */
  public static ChainPaymentEvent of(
      String source,
      String chain,
      String token,
      String tokenAddress,
      String txHash,
      String sourceAddress,
      String destinationAddress,
      BigDecimal amount,
      Long blockNumber) {
    return of(source, chain, token, tokenAddress, txHash, sourceAddress, destinationAddress, amount, blockNumber, null);
  }

  /** 构建带日志序号的链上入账事件，ERC20/SPL 同一 tx 多条转账时用于唯一定位。 */
  public static ChainPaymentEvent of(
      String source,
      String chain,
      String token,
      String tokenAddress,
      String txHash,
      String sourceAddress,
      String destinationAddress,
      BigDecimal amount,
      Long blockNumber,
      Long logIndex) {
    return of(
        source,
        chain,
        token,
        tokenAddress,
        txHash,
        sourceAddress,
        destinationAddress,
        amount,
        blockNumber,
        logIndex,
        null);
  }

  /** 构建带日志序号和链上区块时间的链上入账事件。 */
  public static ChainPaymentEvent of(
      String source,
      String chain,
      String token,
      String tokenAddress,
      String txHash,
      String sourceAddress,
      String destinationAddress,
      BigDecimal amount,
      Long blockNumber,
      Long logIndex,
      Instant blockTimestamp) {
    return new ChainPaymentEvent(
        buildEventId(source, chain, txHash, destinationAddress, tokenAddress),
        source,
        chain,
        token,
        tokenAddress,
        txHash,
        sourceAddress,
        destinationAddress,
        amount,
        blockNumber,
        logIndex,
        blockTimestamp,
        Instant.now());
  }

  private static String buildEventId(
      String source, String chain, String txHash, String destinationAddress, String tokenAddress) {
    String seed =
        String.join(
            ":",
            normalize(source),
            normalize(chain),
            normalize(txHash),
            normalize(destinationAddress),
            normalize(tokenAddress));
    if (seed.replace(":", "").isBlank()) {
      return UUID.randomUUID().toString();
    }
    return seed;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
