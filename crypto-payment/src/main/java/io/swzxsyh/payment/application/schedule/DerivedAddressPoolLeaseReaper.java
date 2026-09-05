package io.swzxsyh.payment.application.schedule;

import io.swzxsyh.payment.channel.address.DerivedAddressPoolService;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Releases derived-address leases after their payment orders finish.
 *
 * <p>The address pool implementation is shared by core, while this scheduled lease cleanup runs only
 * inside the payment application.
 */
@Slf4j
@Component
@ConditionalOnBean(DerivedAddressPoolService.class)
public class DerivedAddressPoolLeaseReaper {

  private final PaymentOrderRepository orderRepository;
  private final DerivedAddressPoolService poolService;

  /**
   * Creates a derived-address lease reaper.
   *
   * @param orderRepository payment order repository
   * @param poolService derived-address pool service
   */
  public DerivedAddressPoolLeaseReaper(
      PaymentOrderRepository orderRepository,
      DerivedAddressPoolService poolService) {
    this.orderRepository = orderRepository;
    this.poolService = poolService;
  }

  /** Releases completed derived-address leases back to the pool or cooldown state. */
  @Scheduled(fixedDelay = 60000)
  public void releaseFinishedLeases() {
    orderRepository.findFinishedDerivedAddressLeases().forEach(order ->
        poolService.release(
            order.getDerivedAddressPoolKey(),
            order.getDerivedAddressLeaseId(),
            order.getStatus().name(),
            order.getPaymentTxHash()));
  }
}
