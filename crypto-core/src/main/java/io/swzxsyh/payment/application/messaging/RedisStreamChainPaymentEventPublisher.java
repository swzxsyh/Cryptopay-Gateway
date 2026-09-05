package io.swzxsyh.payment.application.messaging;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import io.swzxsyh.payment.util.JsonUtil;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes chain payment events through Redis Stream.
 *
 * <p>Redis Stream is the default payment-side MQ adapter because it supports durable messages,
 * consumer groups, pending retry, and multi-node payment consumption.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "crypto.payment.messaging",
    name = "provider",
    havingValue = "REDIS_STREAM",
    matchIfMissing = true)
public class RedisStreamChainPaymentEventPublisher implements ChainPaymentEventPublisher {

  private static final String PAYLOAD_FIELD = "payload";

  private final RedissonClient redissonClient;
  private final CryptoPaymentProperties properties;

  /**
   * Creates the Redis Stream publisher.
   *
   * @param redissonClient Redis client used to append stream messages
   * @param properties payment runtime configuration
   */
  public RedisStreamChainPaymentEventPublisher(
      RedissonClient redissonClient, CryptoPaymentProperties properties) {
    this.redissonClient = redissonClient;
    this.properties = properties;
  }

  /**
   * Serializes and appends one normalized chain payment event to the stream.
   *
   * @param event normalized chain payment event detected by a watcher
   */
  @Override
  public void publish(ChainPaymentEvent event) {
    if (event == null || !StringUtils.hasText(event.txHash())) {
      log.debug("链上入账事件为空或缺少 txHash，跳过写入 Stream。event={}", event);
      return;
    }
    RStream<String, String> stream = stream();
    StreamMessageId messageId =
        stream.add(StreamAddArgs.entries(Map.of(PAYLOAD_FIELD, JsonUtil.toJson(event))));
    log.info(
        "链上入账事件已写入 Redis Stream。stream={}, messageId={}, eventId={}, chain={}, txHash={}",
        properties.getMessaging().getChainPaymentTopic(),
        messageId,
        event.eventId(),
        event.chain(),
        event.txHash());
  }

  private RStream<String, String> stream() {
    return redissonClient.getStream(properties.getMessaging().getChainPaymentTopic(), StringCodec.INSTANCE);
  }
}
