package io.swzxsyh.payment.application.messaging;

import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publishes nothing when the payment application explicitly disables chain payment messaging.
 *
 * <p>This implementation belongs to the payment runtime because it controls how detected chain
 * payments are handed to the payment application. Core only keeps the messaging abstraction.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "crypto.payment.messaging", name = "provider", havingValue = "NONE")
public class NoopChainPaymentEventPublisher implements ChainPaymentEventPublisher {

  /**
   * Drops the event and records a warning for operations troubleshooting.
   *
   * @param event normalized chain payment event detected by a watcher
   */
  @Override
  public void publish(ChainPaymentEvent event) {
    log.warn(
        "链上入账事件没有可用 MQ 实现，已丢弃。eventId={}, chain={}, txHash={}",
        event == null ? null : event.eventId(),
        event == null ? null : event.chain(),
        event == null ? null : event.txHash());
  }
}
