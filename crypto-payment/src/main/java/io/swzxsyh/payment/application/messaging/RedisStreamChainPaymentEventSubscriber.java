package io.swzxsyh.payment.application.messaging;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventSubscriber;
import io.swzxsyh.payment.util.JsonUtil;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.stream.AutoClaimResult;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamMessageId;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Consumes chain payment events through Redis Stream consumer groups.
 *
 * <p>This payment-side adapter supports multi-node competing consumption. Failed messages remain
 * pending and can be claimed by another payment instance after the configured idle window.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "crypto.payment.messaging",
    name = "provider",
    havingValue = "REDIS_STREAM",
    matchIfMissing = true)
public class RedisStreamChainPaymentEventSubscriber implements ChainPaymentEventSubscriber {

  private static final String PAYLOAD_FIELD = "payload";

  private final RedissonClient redissonClient;
  private final CryptoPaymentProperties properties;

  /**
   * Creates the Redis Stream subscriber.
   *
   * @param redissonClient Redis client used to consume stream messages
   * @param properties payment runtime configuration
   */
  public RedisStreamChainPaymentEventSubscriber(
      RedissonClient redissonClient, CryptoPaymentProperties properties) {
    this.redissonClient = redissonClient;
    this.properties = properties;
  }

  /**
   * Starts a background consumer loop for normalized chain payment messages.
   *
   * @param handler callback that handles one decoded chain payment event
   * @return subscription handle used to stop the consumer loop
   */
  @Override
  public Subscription subscribe(Consumer<ChainPaymentEvent> handler) {
    ensureGroup();
    AtomicBoolean running = new AtomicBoolean(true);
    ExecutorService executor =
        Executors.newSingleThreadExecutor(newDaemonThreadFactory("chain-payment-stream-consumer"));
    String consumerName = resolveConsumerName();
    executor.submit(() -> consumeLoop(handler, running, consumerName));
    return () -> {
      running.set(false);
      executor.shutdownNow();
    };
  }

  private void consumeLoop(
      Consumer<ChainPaymentEvent> handler, AtomicBoolean running, String consumerName) {
    String group = properties.getMessaging().getStreamConsumerGroup();
    int batchSize = Math.max(1, properties.getMessaging().getStreamBatchSize());
    Duration pollTimeout =
        Duration.ofMillis(Math.max(100, properties.getMessaging().getStreamPollMillis()));
    log.info(
        "Redis Stream 链上入账消费者循环已启动。stream={}, group={}, consumer={}",
        properties.getMessaging().getChainPaymentTopic(),
        group,
        consumerName);
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        claimAndHandlePending(handler, group, consumerName, batchSize);
        Map<StreamMessageId, Map<String, String>> messages =
            stream()
                .readGroup(
                    group,
                    consumerName,
                    StreamReadGroupArgs.neverDelivered().count(batchSize).timeout(pollTimeout));
        handleMessages(handler, group, messages);
      } catch (Exception ex) {
        if (running.get()) {
          log.error(
              "Redis Stream 链上入账消费循环异常。stream={}, group={}, consumer={}, error={}",
              properties.getMessaging().getChainPaymentTopic(),
              group,
              consumerName,
              ex.getMessage(),
              ex);
          sleepQuietly(1000L);
        }
      }
    }
    log.info(
        "Redis Stream 链上入账消费者循环已停止。stream={}, group={}, consumer={}",
        properties.getMessaging().getChainPaymentTopic(),
        group,
        consumerName);
  }

  private void claimAndHandlePending(
      Consumer<ChainPaymentEvent> handler, String group, String consumerName, int batchSize) {
    int idleSeconds = properties.getMessaging().getStreamPendingClaimIdleSeconds();
    if (idleSeconds <= 0) {
      return;
    }
    AutoClaimResult<String, String> claimed =
        stream().autoClaim(group, consumerName, idleSeconds, TimeUnit.SECONDS, StreamMessageId.MIN, batchSize);
    if (claimed == null || claimed.getMessages() == null || claimed.getMessages().isEmpty()) {
      return;
    }
    log.info(
        "Redis Stream 已认领超时未 ACK 消息。stream={}, group={}, consumer={}, count={}",
        properties.getMessaging().getChainPaymentTopic(),
        group,
        consumerName,
        claimed.getMessages().size());
    handleMessages(handler, group, claimed.getMessages());
  }

  private void handleMessages(
      Consumer<ChainPaymentEvent> handler,
      String group,
      Map<StreamMessageId, Map<String, String>> messages) {
    if (messages == null || messages.isEmpty()) {
      return;
    }
    for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
      StreamMessageId messageId = entry.getKey();
      String payload = entry.getValue() == null ? null : entry.getValue().get(PAYLOAD_FIELD);
      try {
        handler.accept(JsonUtil.fromJson(payload, ChainPaymentEvent.class));
        stream().ack(group, messageId);
        log.debug(
            "Redis Stream 链上入账消息已 ACK。stream={}, group={}, messageId={}",
            properties.getMessaging().getChainPaymentTopic(),
            group,
            messageId);
      } catch (Exception ex) {
        log.error(
            "Redis Stream 链上入账消息处理失败，保留 pending 等待重试。stream={}, group={}, messageId={}, payload={}, error={}",
            properties.getMessaging().getChainPaymentTopic(),
            group,
            messageId,
            payload,
            ex.getMessage(),
            ex);
      }
    }
  }

  private void ensureGroup() {
    try {
      stream()
          .createGroup(
              StreamCreateGroupArgs.name(properties.getMessaging().getStreamConsumerGroup())
                  // XGROUP CREATE only accepts explicit IDs such as 0, 0-0, and $.
                  .id(StreamMessageId.ALL)
                  .makeStream());
      log.info(
          "Redis Stream 消费组已创建。stream={}, group={}",
          properties.getMessaging().getChainPaymentTopic(),
          properties.getMessaging().getStreamConsumerGroup());
    } catch (Exception ex) {
      String message = ex.getMessage() == null ? "" : ex.getMessage();
      if (message.contains("BUSYGROUP")) {
        log.debug(
            "Redis Stream 消费组已存在。stream={}, group={}",
            properties.getMessaging().getChainPaymentTopic(),
            properties.getMessaging().getStreamConsumerGroup());
        return;
      }
      throw ex;
    }
  }

  private String resolveConsumerName() {
    if (StringUtils.hasText(properties.getMessaging().getStreamConsumerName())) {
      return properties.getMessaging().getStreamConsumerName();
    }
    String runtimeName =
        ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^a-zA-Z0-9_.-]", "-");
    return runtimeName + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private ThreadFactory newDaemonThreadFactory(String name) {
    return runnable -> {
      Thread thread = new Thread(runnable, name);
      thread.setDaemon(true);
      return thread;
    };
  }

  private void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private RStream<String, String> stream() {
    return redissonClient.getStream(properties.getMessaging().getChainPaymentTopic(), StringCodec.INSTANCE);
  }
}
