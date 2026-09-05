package io.swzxsyh.payment.application.schedule;

import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import io.swzxsyh.payment.state.PaymentOrderStateMachine;
import java.time.LocalDateTime;
import java.util.EnumSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expires unpaid payment orders.
 *
 * <p>This scheduler belongs to the payment application because it actively changes runtime payment
 * orders. The core module only provides the repository and state machine used here.
 */
@Slf4j
@Component
public class PaymentOrderTimeoutReaper {

  private final PaymentOrderRepository orderRepository;
  private final PaymentOrderStateMachine stateMachine;

  /**
   * Creates a timeout reaper.
   *
   * @param orderRepository payment order repository
   * @param stateMachine payment order state machine
   */
  public PaymentOrderTimeoutReaper(
      PaymentOrderRepository orderRepository,
      PaymentOrderStateMachine stateMachine) {
    this.orderRepository = orderRepository;
    this.stateMachine = stateMachine;
  }

  /** Marks unpaid expired orders as {@link OrderStatus#EXPIRED}. */
  @Scheduled(fixedDelay = 60000)
  public void expireOrders() {
    LocalDateTime now = LocalDateTime.now();
    for (PaymentOrder order : orderRepository.findExpirableOrders(now, 200)) {
      order.setStatus(stateMachine.expired(order.getStatus()));
      boolean updated =
          orderRepository.saveIfStatusIn(
              order,
              EnumSet.of(
                  OrderStatus.CREATED,
                  OrderStatus.METHOD_SELECTED,
                  OrderStatus.WAITING_PAYMENT,
                  OrderStatus.DETECTED));
      if (!updated) {
        log.info(
            "订单超时数据库条件更新未命中，跳过。cryptoOrderNo={}, merchantOrderNo={}",
            order.getCryptoOrderNo(),
            order.getMerchantOrderNo());
        continue;
      }
      log.info(
          "Expired crypto order. cryptoOrderNo={}, merchantOrderNo={}, expireTime={}",
          order.getCryptoOrderNo(),
          order.getMerchantOrderNo(),
          order.getExpireTime());
    }
  }
}
