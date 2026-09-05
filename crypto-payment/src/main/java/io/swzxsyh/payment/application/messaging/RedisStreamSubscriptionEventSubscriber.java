package io.swzxsyh.payment.application.messaging;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.messaging.SubscriptionChainEvent;
import io.swzxsyh.payment.subscription.SubscriptionBillingService;
import io.swzxsyh.payment.subscription.SubscriptionOrderService;
import io.swzxsyh.payment.util.JsonUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
 * 订阅合约事件 Redis Stream 消费器。
 *
 * <p>该类只做消息解码和事件分发，实际状态流转仍交给订阅订单服务和订阅账单服务，避免 watcher
 * 或消息层绕过领域状态机。
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "crypto.payment.messaging",
    name = "provider",
    havingValue = "REDIS_STREAM",
    matchIfMissing = true)
public class RedisStreamSubscriptionEventSubscriber {

  private static final String PAYLOAD_FIELD = "payload";

  private final RedissonClient redissonClient;
  private final CryptoPaymentProperties properties;
  private final SubscriptionOrderService subscriptionOrderService;
  private final SubscriptionBillingService subscriptionBillingService;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private ExecutorService executor;

  public RedisStreamSubscriptionEventSubscriber(
      RedissonClient redissonClient,
      CryptoPaymentProperties properties,
      SubscriptionOrderService subscriptionOrderService,
      SubscriptionBillingService subscriptionBillingService) {
    this.redissonClient = redissonClient;
    this.properties = properties;
    this.subscriptionOrderService = subscriptionOrderService;
    this.subscriptionBillingService = subscriptionBillingService;
  }

  /** 启动订阅事件消费循环。 */
  @PostConstruct
  public void start() {
    if (!properties.getMessaging().isConsumerEnabled()) {
      log.info("订阅事件消费者未启用。stream={}", properties.getMessaging().getSubscriptionEventTopic());
      return;
    }
    ensureGroup();
    running.set(true);
    executor = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "subscription-event-stream-consumer");
      thread.setDaemon(true);
      return thread;
    });
    executor.submit(this::consumeLoop);
  }

  /** 停止订阅事件消费循环。 */
  @PreDestroy
  public void stop() {
    running.set(false);
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  private void consumeLoop() {
    String group = subscriptionGroup();
    String consumerName = resolveConsumerName();
    int batchSize = Math.max(1, properties.getMessaging().getStreamBatchSize());
    Duration pollTimeout =
        Duration.ofMillis(Math.max(100L, properties.getMessaging().getStreamPollMillis()));
    log.info(
        "Redis Stream 订阅事件消费者已启动。stream={}, group={}, consumer={}",
        properties.getMessaging().getSubscriptionEventTopic(),
        group,
        consumerName);
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        claimPending(group, consumerName, batchSize);
        Map<StreamMessageId, Map<String, String>> messages =
            stream()
                .readGroup(
                    group,
                    consumerName,
                    StreamReadGroupArgs.neverDelivered().count(batchSize).timeout(pollTimeout));
        handleMessages(group, messages);
      } catch (Exception ex) {
        if (running.get()) {
          log.error(
              "Redis Stream 订阅事件消费异常。stream={}, group={}, error={}",
              properties.getMessaging().getSubscriptionEventTopic(),
              group,
              ex.getMessage(),
              ex);
          sleepQuietly();
        }
      }
    }
  }

  private void claimPending(String group, String consumerName, int batchSize) {
    int idleSeconds = properties.getMessaging().getStreamPendingClaimIdleSeconds();
    if (idleSeconds <= 0) {
      return;
    }
    AutoClaimResult<String, String> claimed =
        stream().autoClaim(group, consumerName, idleSeconds, TimeUnit.SECONDS, StreamMessageId.MIN, batchSize);
    if (claimed != null && claimed.getMessages() != null && !claimed.getMessages().isEmpty()) {
      handleMessages(group, claimed.getMessages());
    }
  }

  private void handleMessages(String group, Map<StreamMessageId, Map<String, String>> messages) {
    if (messages == null || messages.isEmpty()) {
      return;
    }
    for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
      String payload = entry.getValue() == null ? null : entry.getValue().get(PAYLOAD_FIELD);
      try {
        handle(JsonUtil.fromJson(payload, SubscriptionChainEvent.class));
        stream().ack(group, entry.getKey());
      } catch (Exception ex) {
        log.error(
            "订阅链上事件处理失败，保留 pending 等待重试。messageId={}, payload={}, error={}",
            entry.getKey(),
            payload,
            ex.getMessage(),
            ex);
      }
    }
  }

  private void handle(SubscriptionChainEvent event) {
    if (event == null || !StringUtils.hasText(event.eventType())) {
      return;
    }
    String type = event.eventType().trim().toUpperCase();
    boolean handled =
        switch (type) {
          case "ACTIVATED" -> subscriptionOrderService.activateByEventId(event.subscriptionId());
          case "BILLING_PAID" -> subscriptionBillingService.confirmByTxHash(event.txHash(), event.blockNumber());
          case "PAUSED" -> subscriptionOrderService.pauseByEventId(event.subscriptionId());
          case "RESUMED" -> subscriptionOrderService.resumeByEventId(event.subscriptionId());
          case "CANCELLED" -> subscriptionOrderService.cancelByEventId(event.subscriptionId());
          default -> false;
        };
    log.info(
        "订阅链上事件已消费。eventType={}, subscriptionId={}, txHash={}, handled={}",
        type,
        event.subscriptionId(),
        event.txHash(),
        handled);
  }

  private void ensureGroup() {
    try {
      stream()
          .createGroup(
              StreamCreateGroupArgs.name(subscriptionGroup())
                  .id(StreamMessageId.ALL)
                  .makeStream());
      log.info(
          "Redis Stream 订阅事件消费组已创建。stream={}, group={}",
          properties.getMessaging().getSubscriptionEventTopic(),
          subscriptionGroup());
    } catch (Exception ex) {
      String message = ex.getMessage() == null ? "" : ex.getMessage();
      if (!message.contains("BUSYGROUP")) {
        throw ex;
      }
    }
  }

  private String subscriptionGroup() {
    return properties.getMessaging().getStreamConsumerGroup() + "-subscription";
  }

  private String resolveConsumerName() {
    if (StringUtils.hasText(properties.getMessaging().getStreamConsumerName())) {
      return properties.getMessaging().getStreamConsumerName() + "-subscription";
    }
    return ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^a-zA-Z0-9_.-]", "-")
        + "-"
        + UUID.randomUUID().toString().substring(0, 8);
  }

  private RStream<String, String> stream() {
    return redissonClient.getStream(properties.getMessaging().getSubscriptionEventTopic(), StringCodec.INSTANCE);
  }

  private void sleepQuietly() {
    try {
      Thread.sleep(1000L);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }
}
