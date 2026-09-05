package io.swzxsyh.payment.application.subscription;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionBillingRepository;
import io.swzxsyh.payment.subscription.SubscriptionBillingService;
import io.swzxsyh.payment.subscription.SubscriptionBillingStatus;
import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.SubscriptionOrderRepository;
import io.swzxsyh.payment.subscription.SubscriptionStatus;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Schedules recurring subscription billing.
 *
 * <p>This payment-application scheduler orchestrates core subscription services and uses a Redis lock
 * so only one node creates or executes bills in each scheduling window.
 */
@Slf4j
@Service
public class SubscriptionBillingScheduler {

  private final CryptoPaymentProperties properties;
  private final SubscriptionOrderRepository orderRepository;
  private final SubscriptionBillingRepository billingRepository;
  private final SubscriptionBillingService billingService;
  private final LockUtil lockUtil;

  /**
   * Creates a subscription billing scheduler.
   *
   * @param properties payment runtime properties
   * @param orderRepository subscription order repository
   * @param billingRepository subscription billing repository
   * @param billingService subscription billing service
   * @param lockUtil Redis lock utility
   */
  public SubscriptionBillingScheduler(
      CryptoPaymentProperties properties,
      SubscriptionOrderRepository orderRepository,
      SubscriptionBillingRepository billingRepository,
      SubscriptionBillingService billingService,
      LockUtil lockUtil) {
    this.properties = properties;
    this.orderRepository = orderRepository;
    this.billingRepository = billingRepository;
    this.billingService = billingService;
    this.lockUtil = lockUtil;
  }

  /** Creates due subscription bills and retries executable failed bills. */
  @Scheduled(fixedDelayString = "#{T(java.lang.Math).max(1, @cryptoPaymentProperties.subscription.schedulerIntervalSeconds) * 1000}")
  public void scheduleDueBillings() {
    if (!properties.getSubscription().isEnabled()
        || !properties.getSubscription().isSchedulerEnabled()) {
      return;
    }
    lockUtil.withLock(
        RedisKeyNamespace.subscriptionBillingSchedulerLock(properties),
        1000,
        25,
        () -> {
          LocalDateTime now = LocalDateTime.now();
          int limit = Math.max(1, properties.getSubscription().getSchedulerBatchSize());
          for (SubscriptionOrder order : orderRepository.findDueActiveOrders(now, limit)) {
            if (order.getBillingMode() == SubscriptionBillingMode.SUPERFLUID_STREAM) {
              billingService.settleStreamWindow(order, now);
              continue;
            }
            SubscriptionBillingRecord bill = billingService.createDueBill(order, now);
            billingService.executeBilling(order, bill);
          }
          for (SubscriptionBillingRecord bill : billingRepository.findExecutableBills(now, limit)) {
            if (bill.getStatus() == SubscriptionBillingStatus.FAILED
                && (bill.getRetryCount() == null
                    || bill.getRetryCount() >= properties.getSubscription().getMaxRetryCount())) {
              continue;
            }
            orderRepository.findBySubscriptionOrderNo(bill.getSubscriptionOrderNo())
                .filter(order -> order.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(order -> order.getBillingMode() != SubscriptionBillingMode.SUPERFLUID_STREAM)
                .ifPresent(order -> billingService.executeBilling(order, bill));
          }
          return null;
        });
  }
}
