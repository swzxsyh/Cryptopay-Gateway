package io.swzxsyh.payment.application.messaging;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventSubscriber;
import io.swzxsyh.payment.util.JsonUtil;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes chain payment events from Redis Pub/Sub.
 *
 * <p>This adapter is payment-runtime specific. It should only be loaded by the payment application
 * so manager nodes cannot accidentally consume payment recognition messages.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "crypto.payment.messaging", name = "provider", havingValue = "REDIS")
public class RedisChainPaymentEventSubscriber implements ChainPaymentEventSubscriber {

  private final RedissonClient redissonClient;
  private final CryptoPaymentProperties properties;

  /**
   * Creates the Redis Pub/Sub subscriber.
   *
   * @param redissonClient Redis client used to listen on the topic
   * @param properties payment runtime configuration
   */
  public RedisChainPaymentEventSubscriber(
      RedissonClient redissonClient, CryptoPaymentProperties properties) {
    this.redissonClient = redissonClient;
    this.properties = properties;
  }

  /**
   * Subscribes a handler to normalized chain payment messages.
   *
   * @param handler callback that receives one decoded chain payment event
   * @return subscription handle used to remove the listener
   */
  @Override
  public Subscription subscribe(Consumer<ChainPaymentEvent> handler) {
    RTopic topic = topic();
    int listenerId =
        topic.addListener(
            String.class,
            (channel, payload) -> {
              try {
                handler.accept(JsonUtil.fromJson(payload, ChainPaymentEvent.class));
              } catch (Exception ex) {
                log.error(
                    "Redis 链上入账消息反序列化或分发失败。topic={}, payload={}, error={}",
                    channel,
                    payload,
                    ex.getMessage(),
                    ex);
              }
            });
    log.info(
        "Redis 链上入账消息订阅已建立。topic={}, listenerId={}",
        properties.getMessaging().getChainPaymentTopic(),
        listenerId);
    return () -> {
      topic().removeListener(listenerId);
      log.info(
          "Redis 链上入账消息订阅已关闭。topic={}, listenerId={}",
          properties.getMessaging().getChainPaymentTopic(),
          listenerId);
    };
  }

  private RTopic topic() {
    return redissonClient.getTopic(properties.getMessaging().getChainPaymentTopic(), StringCodec.INSTANCE);
  }
}
