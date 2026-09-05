package io.swzxsyh.payment.application.messaging;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import io.swzxsyh.payment.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Publishes chain payment events through Redis Pub/Sub.
 *
 * <p>Pub/Sub is a lightweight broadcast implementation. It is kept as a payment-side adapter for
 * development or simple deployments, while durable consumption should prefer Redis Stream.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "crypto.payment.messaging", name = "provider", havingValue = "REDIS")
public class RedisChainPaymentEventPublisher implements ChainPaymentEventPublisher {

  private final RedissonClient redissonClient;
  private final CryptoPaymentProperties properties;

  /**
   * Creates the Redis Pub/Sub publisher.
   *
   * @param redissonClient Redis client used to publish topic messages
   * @param properties payment runtime configuration
   */
  public RedisChainPaymentEventPublisher(
      RedissonClient redissonClient, CryptoPaymentProperties properties) {
    this.redissonClient = redissonClient;
    this.properties = properties;
  }

  /**
   * Serializes and broadcasts one normalized chain payment event.
   *
   * @param event normalized chain payment event detected by a watcher
   */
  @Override
  public void publish(ChainPaymentEvent event) {
    if (event == null || !StringUtils.hasText(event.txHash())) {
      log.debug("链上入账事件为空或缺少 txHash，跳过发布。event={}", event);
      return;
    }
    RTopic topic =
        redissonClient.getTopic(properties.getMessaging().getChainPaymentTopic(), StringCodec.INSTANCE);
    long receivers = topic.publish(JsonUtil.toJson(event));
    log.info(
        "链上入账事件已发布到 Redis Pub/Sub。topic={}, receivers={}, eventId={}, chain={}, txHash={}",
        properties.getMessaging().getChainPaymentTopic(),
        receivers,
        event.eventId(),
        event.chain(),
        event.txHash());
  }
}
