package io.swzxsyh.payment.application.subscription;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionBillingRepository;
import io.swzxsyh.payment.subscription.SubscriptionBillingService;
import io.swzxsyh.payment.subscription.SubscriptionBillingStatus;
import io.swzxsyh.payment.subscription.SubscriptionOrderRepository;
import io.swzxsyh.payment.subscription.SubscriptionOrderService;
import io.swzxsyh.payment.subscription.SubscriptionStatus;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.math.BigInteger;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

/**
 * Confirms subscription setup and billing transaction receipts.
 *
 * <p>This is a payment runtime compensator. It catches cases where contract events are delayed or
 * missed, but it still delegates business state changes to core services and repositories.
 */
@Slf4j
@Service
public class SubscriptionReceiptConfirmationService {

  private final CryptoPaymentProperties properties;
  private final SubscriptionOrderRepository orderRepository;
  private final SubscriptionBillingRepository billingRepository;
  private final SubscriptionOrderService orderService;
  private final SubscriptionBillingService billingService;
  private final ChainClientFactory chainClientFactory;
  private final LockUtil lockUtil;

  /**
   * Creates a subscription receipt confirmation service.
   *
   * @param properties payment runtime properties
   * @param orderRepository subscription order repository
   * @param billingRepository subscription billing repository
   * @param orderService subscription order service
   * @param billingService subscription billing service
   * @param chainClientFactory chain client factory
   * @param lockUtil Redis lock utility
   */
  public SubscriptionReceiptConfirmationService(
      CryptoPaymentProperties properties,
      SubscriptionOrderRepository orderRepository,
      SubscriptionBillingRepository billingRepository,
      SubscriptionOrderService orderService,
      SubscriptionBillingService billingService,
      ChainClientFactory chainClientFactory,
      LockUtil lockUtil) {
    this.properties = properties;
    this.orderRepository = orderRepository;
    this.billingRepository = billingRepository;
    this.orderService = orderService;
    this.billingService = billingService;
    this.chainClientFactory = chainClientFactory;
    this.lockUtil = lockUtil;
  }

  /** Polls receipts for subscription setup and billing transactions. */
  @Scheduled(fixedDelay = 20000)
  public void confirmReceipts() {
    if (!properties.getSubscription().isEnabled()) {
      return;
    }
    lockUtil.withLock(
        RedisKeyNamespace.subscriptionReceiptConfirmationLock(properties),
        1000,
        18,
        () -> {
          confirmActivatingOrders();
          confirmExecutingBills();
          return null;
        });
  }

  private void confirmActivatingOrders() {
    orderRepository
        .findActivatingOrdersWithSetupTx(
            Math.max(1, properties.getSubscription().getSchedulerBatchSize()))
        .forEach(order -> {
          TransactionReceipt receipt = receipt(order.getChain(), order.getSetupTxHash());
          if (isSuccess(receipt)) {
            orderService.activate(order.getSubscriptionOrderNo());
            log.info("订阅初始化交易确认成功。subscriptionOrderNo={}, txHash={}, blockNumber={}",
                order.getSubscriptionOrderNo(), order.getSetupTxHash(), receipt.getBlockNumber());
          } else if (isFailed(receipt)) {
            order.setStatus(SubscriptionStatus.FAILED);
            order.setFailureReason("setup transaction failed on chain");
            orderRepository.saveIfStatusIn(order, Set.of(SubscriptionStatus.ACTIVATING));
            log.warn("订阅初始化交易链上失败。subscriptionOrderNo={}, txHash={}",
                order.getSubscriptionOrderNo(), order.getSetupTxHash());
          }
        });
  }

  private void confirmExecutingBills() {
    int limit = Math.max(1, properties.getSubscription().getSchedulerBatchSize());
    for (var bill : billingRepository.findExecutingBills(limit)) {
      TransactionReceipt receipt = receipt(bill.getChain(), bill.getExecutionTxHash());
      if (isSuccess(receipt)) {
        billingService.confirmPaid(
            bill,
            bill.getExecutionTxHash(),
            bill.getAmount(),
            receipt.getBlockNumber() == null ? null : receipt.getBlockNumber().longValue());
      } else if (isFailed(receipt)) {
        bill.setStatus(SubscriptionBillingStatus.FAILED);
        bill.setFailureReason("billing transaction failed on chain");
        bill.setNextRetryAt(java.time.LocalDateTime.now().plusSeconds(
            properties.getSubscription().getRetryBackoffSeconds()));
        billingRepository.saveIfStatusIn(bill, Set.of(SubscriptionBillingStatus.EXECUTING));
        log.warn("订阅扣款交易链上失败。subscriptionOrderNo={}, billingSequence={}, txHash={}",
            bill.getSubscriptionOrderNo(), bill.getBillingSequence(), bill.getExecutionTxHash());
      }
    }
  }

  private TransactionReceipt receipt(String chain, String txHash) {
    if (!chainClientFactory.get(chain).isEvmFamily()) {
      return null;
    }
    return chainClientFactory.get(chain).getTransactionReceipt(txHash);
  }

  private boolean isSuccess(TransactionReceipt receipt) {
    return receipt != null && "0x1".equalsIgnoreCase(receipt.getStatus());
  }

  private boolean isFailed(TransactionReceipt receipt) {
    if (receipt == null || receipt.getStatus() == null) {
      return false;
    }
    BigInteger status = Numeric.toBigInt(receipt.getStatus());
    return BigInteger.ZERO.equals(status);
  }
}
