package io.swzxsyh.payment.messaging;

/** 链上入账事件发布端口；后续可替换为 Kafka、RocketMQ、RabbitMQ 等实现。 */
public interface ChainPaymentEventPublisher {

  /** 发布一条链上入账事件，消费者负责订单认账、隔离和回调。 */
  void publish(ChainPaymentEvent event);
}
