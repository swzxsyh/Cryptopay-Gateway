package io.swzxsyh.payment.subscription;

import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.callback.PaymentCallbackDeliveryService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.accounting.MerchantBalanceLedgerService;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeCalculator;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeResult;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeSnapshot;
import io.swzxsyh.payment.subscription.contract.SubscriptionContractAdapterRegistry;
import io.swzxsyh.payment.subscription.contract.SubscriptionExecutionResult;
import io.swzxsyh.payment.subscription.dto.SubscriptionPaymentNotificationRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 订阅周期账单服务，负责生成、执行、确认和回调。 */
@Slf4j
@Service
public class SubscriptionBillingService {

  private final SubscriptionBillingRepository billingRepository;
  private final SubscriptionOrderRepository orderRepository;
  private final SubscriptionContractAdapterRegistry adapterRegistry;
  private final CryptoPaymentProperties properties;
  private final PaymentCallbackDeliveryService callbackDeliveryService;
  private final PaymentAuditService auditService;
  private final MerchantSettlementFeeCalculator settlementFeeCalculator;
  private final MerchantBalanceLedgerService merchantBalanceLedgerService;
  private final SubscriptionKytGuard subscriptionKytGuard;

  public SubscriptionBillingService(
      SubscriptionBillingRepository billingRepository,
      SubscriptionOrderRepository orderRepository,
      SubscriptionContractAdapterRegistry adapterRegistry,
      CryptoPaymentProperties properties,
      PaymentCallbackDeliveryService callbackDeliveryService,
      PaymentAuditService auditService,
      MerchantSettlementFeeCalculator settlementFeeCalculator,
      MerchantBalanceLedgerService merchantBalanceLedgerService,
      SubscriptionKytGuard subscriptionKytGuard) {
    this.billingRepository = billingRepository;
    this.orderRepository = orderRepository;
    this.adapterRegistry = adapterRegistry;
    this.properties = properties;
    this.callbackDeliveryService = callbackDeliveryService;
    this.auditService = auditService;
    this.settlementFeeCalculator = settlementFeeCalculator;
    this.merchantBalanceLedgerService = merchantBalanceLedgerService;
    this.subscriptionKytGuard = subscriptionKytGuard;
  }

  /** 为到期订阅生成下一期账单；若本订阅已有未完成账单，则复用该账单。 */
  public SubscriptionBillingRecord createDueBill(SubscriptionOrder order, LocalDateTime now) {
    return billingRepository.findLatestBySubscriptionOrderNo(order.getSubscriptionOrderNo())
        .filter(record -> record.getStatus() == SubscriptionBillingStatus.PENDING
            || record.getStatus() == SubscriptionBillingStatus.EXECUTING
            || record.getStatus() == SubscriptionBillingStatus.KYT_REVIEW
            || record.getStatus() == SubscriptionBillingStatus.FAILED)
        .orElseGet(() -> createNewBill(order, now));
  }

  /** 执行一期订阅扣款。 */
  public SubscriptionBillingRecord executeBilling(SubscriptionOrder order, SubscriptionBillingRecord bill) {
    try {
      SubscriptionExecutionResult result =
          adapterRegistry.get(order.getBillingMode()).executeBilling(order, bill);
      if (!result.submitted()) {
        markFailed(bill, result.message());
        return bill;
      }
      bill.setStatus(SubscriptionBillingStatus.EXECUTING);
      bill.setExecutionTxHash(result.txHash());
      bill.setFailureReason(null);
      saveBillStatusChange(bill, Set.of(SubscriptionBillingStatus.PENDING, SubscriptionBillingStatus.FAILED));
      auditService.record("SUBSCRIPTION_BILLING_SUBMITTED", "SUBSCRIPTION_BILLING",
          bill.getId().toString(), "EXECUTING", bill);
      log.info("订阅扣款交易已提交。subscriptionOrderNo={}, billingSequence={}, txHash={}",
          bill.getSubscriptionOrderNo(), bill.getBillingSequence(), result.txHash());
      return bill;
    } catch (Exception ex) {
      markFailed(bill, ex.getMessage());
      return bill;
    }
  }

  /**
   * 结算 Superfluid 真流式支付窗口。
   *
   * <p>真流式支付不是每秒由 Java 扣一次款，而是链上持续流动；这里按结算窗口把
   * 已经过账的时间折算成金额，生成一条已支付账单，再进入商户余额和资金流水。
   */
  @Transactional(rollbackFor = Exception.class)
  public Optional<SubscriptionBillingRecord> settleStreamWindow(SubscriptionOrder order, LocalDateTime now) {
    if (order == null || order.getBillingMode() != SubscriptionBillingMode.SUPERFLUID_STREAM) {
      return Optional.empty();
    }
    if (order.getStatus() != SubscriptionStatus.ACTIVE) {
      return Optional.empty();
    }
    LocalDateTime settlementStart = streamSettlementStart(order);
    if (settlementStart == null || !now.isAfter(settlementStart)) {
      return Optional.empty();
    }
    long elapsedSeconds = Math.max(0, Duration.between(settlementStart, now).getSeconds());
    if (elapsedSeconds <= 0) {
      return Optional.empty();
    }
    BigDecimal amount = proratedStreamAmount(order, elapsedSeconds);
    if (amount.signum() <= 0) {
      log.debug("流式订阅窗口金额为 0，跳过结算。subscriptionOrderNo={}, elapsedSeconds={}",
          order.getSubscriptionOrderNo(), elapsedSeconds);
      return Optional.empty();
    }

    SubscriptionBillingRecord bill = createStreamBill(order, now, amount);
    var kytResult = subscriptionKytGuard.postcheckBilling(order, bill, null, amount);
    if (subscriptionKytGuard.requiresManualHandling(kytResult)) {
      markKytReview(bill, subscriptionKytGuard.reason(kytResult), null, null, amount);
      log.warn("流式订阅窗口命中 KYT，已进入人工复核，不入账不回调。subscriptionOrderNo={}, billingSequence={}, decision={}, riskScore={}",
          order.getSubscriptionOrderNo(), bill.getBillingSequence(), kytResult.decision(), kytResult.riskScore());
      return Optional.of(bill);
    }
    markPaidAfterKytApproved(bill, null, amount, null, now);
    merchantBalanceLedgerService.creditSubscriptionBilling(order, bill);
    order.setLastBillingAt(now);
    order.setNextBillingAt(now.plusSeconds(Math.max(1, properties.getSubscription().getStreamSettlementIntervalSeconds())));
    if (!orderRepository.saveIfStatusIn(order, Set.of(SubscriptionStatus.ACTIVE))) {
      throw new IllegalStateException("Subscription status changed concurrently: " + order.getSubscriptionOrderNo());
    }

    SubscriptionPaymentNotificationRecord notification =
        new SubscriptionPaymentNotificationRecord(
            order.getSubscriptionOrderNo(),
            order.getMerchantOrderNo(),
            order.getMerchantId(),
            bill.getBillingSequence(),
            bill.getChain(),
            bill.getToken(),
            null,
            bill.getAmount(),
            bill.getRealAmount(),
            null,
            bill.getTransactionFee(),
            bill.getTaxFee(),
            bill.getTotalFee(),
            bill.getSettlementAmount(),
            order.getStatus().name(),
            bill.getStatus().name());
    callbackDeliveryService.enqueuePayloadAndDispatch(
        "SUBSCRIPTION_STREAM_SETTLED",
        order.getMerchantId(),
        order.getChain(),
        order.getSubscriptionOrderNo(),
        order.getMerchantOrderNo(),
        order.getNotifyUrl(),
        notification);
    auditService.record("SUBSCRIPTION_STREAM_SETTLED", "SUBSCRIPTION_BILLING",
        bill.getId().toString(), "PAID", notification);
    log.info("流式订阅窗口已结算。subscriptionOrderNo={}, billingSequence={}, elapsedSeconds={}, amount={}, settlementAmount={}",
        order.getSubscriptionOrderNo(),
        bill.getBillingSequence(),
        elapsedSeconds,
        bill.getRealAmount(),
        bill.getSettlementAmount());
    return Optional.of(bill);
  }

  /** 确认一期账单已经链上成功。 */
  @Transactional(rollbackFor = Exception.class)
  public SubscriptionBillingRecord confirmPaid(
      SubscriptionBillingRecord bill, String txHash, BigDecimal realAmount, Long blockNumber) {
    SubscriptionOrder order = orderRepository.findBySubscriptionOrderNo(bill.getSubscriptionOrderNo())
        .orElseThrow(() -> new IllegalArgumentException("Subscription order not found: " + bill.getSubscriptionOrderNo()));
    LocalDateTime now = LocalDateTime.now();
    BigDecimal confirmedAmount = realAmount == null ? bill.getAmount() : realAmount;
    var kytResult = subscriptionKytGuard.postcheckBilling(order, bill, txHash, confirmedAmount);
    if (subscriptionKytGuard.requiresManualHandling(kytResult)) {
      markKytReview(bill, subscriptionKytGuard.reason(kytResult), txHash, blockNumber, confirmedAmount);
      log.warn("订阅账单命中 KYT，已进入人工复核，不入账不回调。subscriptionOrderNo={}, billingSequence={}, txHash={}, decision={}, riskScore={}",
          order.getSubscriptionOrderNo(),
          bill.getBillingSequence(),
          txHash,
          kytResult.decision(),
          kytResult.riskScore());
      return bill;
    }
    markPaidAfterKytApproved(bill, txHash, confirmedAmount, blockNumber, now);

    order.setLastBillingAt(now);
    order.setNextBillingAt(order.getNextBillingAt().plusSeconds(order.getCycleSeconds()));
    if (!orderRepository.saveIfStatusIn(order, Set.of(SubscriptionStatus.ACTIVE))) {
      log.warn("订阅账单已确认，但订阅主单状态已变化，跳过下次扣款时间推进。subscriptionOrderNo={}, billingSequence={}",
          order.getSubscriptionOrderNo(), bill.getBillingSequence());
    }

    merchantBalanceLedgerService.creditSubscriptionBilling(order, bill);

    SubscriptionPaymentNotificationRecord notification =
        new SubscriptionPaymentNotificationRecord(
            order.getSubscriptionOrderNo(),
            order.getMerchantOrderNo(),
            order.getMerchantId(),
            bill.getBillingSequence(),
            bill.getChain(),
            bill.getToken(),
            txHash,
            bill.getAmount(),
            bill.getRealAmount(),
            blockNumber,
            bill.getTransactionFee(),
            bill.getTaxFee(),
            bill.getTotalFee(),
            bill.getSettlementAmount(),
            order.getStatus().name(),
            bill.getStatus().name());
    callbackDeliveryService.enqueuePayloadAndDispatch(
        "SUBSCRIPTION_BILLING_PAID",
        order.getMerchantId(),
        order.getChain(),
        order.getSubscriptionOrderNo(),
        order.getMerchantOrderNo(),
        order.getNotifyUrl(),
        notification);
    auditService.record("SUBSCRIPTION_BILLING_PAID", "SUBSCRIPTION_BILLING",
        bill.getId().toString(), "PAID", notification);
    log.info("订阅账单已确认并入队回调。subscriptionOrderNo={}, billingSequence={}, txHash={}",
        order.getSubscriptionOrderNo(), bill.getBillingSequence(), txHash);
    return bill;
  }

  private void markPaidAfterKytApproved(
      SubscriptionBillingRecord bill, String txHash, BigDecimal realAmount, Long blockNumber, LocalDateTime now) {
    bill.setStatus(SubscriptionBillingStatus.PAID);
    bill.setExecutionTxHash(txHash);
    bill.setRealAmount(realAmount == null ? bill.getAmount() : realAmount);
    bill.setConfirmedBlockNumber(blockNumber);
    bill.setPaidAt(now);
    bill.setFailureReason(null);
    applySettlementFeeResult(bill);
    saveBillStatusChange(
        bill,
        Set.of(
            SubscriptionBillingStatus.PENDING,
            SubscriptionBillingStatus.EXECUTING,
            SubscriptionBillingStatus.FAILED));
  }

  /** 按交易哈希确认账单。 */
  public boolean confirmByTxHash(String txHash, Long blockNumber) {
    return billingRepository.findByExecutionTxHash(txHash)
        .map(bill -> {
          confirmPaid(bill, txHash, bill.getAmount(), blockNumber);
          return true;
        })
        .orElse(false);
  }

  private SubscriptionBillingRecord createNewBill(SubscriptionOrder order, LocalDateTime now) {
    int nextSequence = billingRepository.findLatestBySubscriptionOrderNo(order.getSubscriptionOrderNo())
        .map(SubscriptionBillingRecord::getBillingSequence)
        .orElse(0) + 1;
    SubscriptionBillingRecord bill = new SubscriptionBillingRecord();
    bill.setSubscriptionOrderNo(order.getSubscriptionOrderNo());
    bill.setMerchantId(order.getMerchantId());
    bill.setMerchantOrderNo(order.getMerchantOrderNo());
    bill.setBillingSequence(nextSequence);
    bill.setAmount(order.getAmountPerCycle());
    bill.setCurrency(order.getCurrency());
    bill.setChain(order.getChain());
    bill.setToken(order.getToken());
    bill.setTokenAddress(order.getTokenAddress());
    bill.setTransactionFeeRate(order.getTransactionFeeRate());
    bill.setMinimumFee(order.getMinimumFee());
    bill.setFixedFee(order.getFixedFee());
    bill.setGatewayFee(order.getGatewayFee());
    bill.setTaxRate(order.getTaxRate());
    bill.setFeeSettlementMode(order.getFeeSettlementMode());
    bill.setDueAt(order.getNextBillingAt() == null ? now : order.getNextBillingAt());
    bill.setStatus(SubscriptionBillingStatus.PENDING);
    bill.setRetryCount(0);
    billingRepository.save(bill);
    auditService.record("SUBSCRIPTION_BILLING_CREATED", "SUBSCRIPTION_BILLING",
        bill.getId().toString(), "PENDING", bill);
    log.info("订阅账单已生成。subscriptionOrderNo={}, billingSequence={}, dueAt={}",
        order.getSubscriptionOrderNo(), nextSequence, bill.getDueAt());
    return bill;
  }

  private SubscriptionBillingRecord createStreamBill(
      SubscriptionOrder order, LocalDateTime now, BigDecimal amount) {
    int nextSequence = billingRepository.findLatestBySubscriptionOrderNo(order.getSubscriptionOrderNo())
        .map(SubscriptionBillingRecord::getBillingSequence)
        .orElse(0) + 1;
    SubscriptionBillingRecord bill = new SubscriptionBillingRecord();
    bill.setSubscriptionOrderNo(order.getSubscriptionOrderNo());
    bill.setMerchantId(order.getMerchantId());
    bill.setMerchantOrderNo(order.getMerchantOrderNo());
    bill.setBillingSequence(nextSequence);
    bill.setAmount(amount);
    bill.setCurrency(order.getCurrency());
    bill.setChain(order.getChain());
    bill.setToken(order.getToken());
    bill.setTokenAddress(order.getTokenAddress());
    bill.setTransactionFeeRate(order.getTransactionFeeRate());
    bill.setMinimumFee(order.getMinimumFee());
    bill.setFixedFee(order.getFixedFee());
    bill.setGatewayFee(order.getGatewayFee());
    bill.setTaxRate(order.getTaxRate());
    bill.setFeeSettlementMode(order.getFeeSettlementMode());
    bill.setDueAt(now);
    bill.setStatus(SubscriptionBillingStatus.EXECUTING);
    bill.setRealAmount(amount);
    bill.setRetryCount(0);
    billingRepository.save(bill);
    auditService.record("SUBSCRIPTION_STREAM_BILLING_CREATED", "SUBSCRIPTION_BILLING",
        bill.getId().toString(), "EXECUTING", bill);
    return bill;
  }

  private LocalDateTime streamSettlementStart(SubscriptionOrder order) {
    if (order.getLastBillingAt() != null) {
      return order.getLastBillingAt();
    }
    if (order.getActivatedAt() != null) {
      return order.getActivatedAt();
    }
    return order.getCreatedAt();
  }

  private BigDecimal proratedStreamAmount(SubscriptionOrder order, long elapsedSeconds) {
    BigDecimal amountPerCycle = order.getAmountPerCycle() == null ? BigDecimal.ZERO : order.getAmountPerCycle();
    return amountPerCycle
        .multiply(BigDecimal.valueOf(elapsedSeconds))
        .divide(BigDecimal.valueOf(Math.max(1, order.getCycleSeconds())), 18, RoundingMode.HALF_UP);
  }

  /** 将订阅账单置为 KYT 人工复核态；该状态不会自动入账、回调或重试扣款。 */
  private void markKytReview(
      SubscriptionBillingRecord bill,
      String reason,
      String txHash,
      Long blockNumber,
      BigDecimal realAmount) {
    bill.setStatus(SubscriptionBillingStatus.KYT_REVIEW);
    bill.setExecutionTxHash(txHash);
    bill.setConfirmedBlockNumber(blockNumber);
    bill.setRealAmount(realAmount == null ? bill.getAmount() : realAmount);
    bill.setPaidAt(null);
    bill.setNextRetryAt(null);
    bill.setFailureReason(reason);
    saveBillStatusChange(
        bill,
        Set.of(
            SubscriptionBillingStatus.PENDING,
            SubscriptionBillingStatus.EXECUTING,
            SubscriptionBillingStatus.FAILED));
    auditService.record("SUBSCRIPTION_BILLING_KYT_REVIEW", "SUBSCRIPTION_BILLING",
        bill.getId().toString(), "KYT_REVIEW", bill);
  }

  private void applySettlementFeeResult(SubscriptionBillingRecord bill) {
    MerchantSettlementFeeResult result =
        settlementFeeCalculator.calculate(
            bill.getRealAmount() == null ? bill.getAmount() : bill.getRealAmount(),
            new MerchantSettlementFeeSnapshot(
                bill.getTransactionFeeRate(),
                bill.getMinimumFee(),
                bill.getFixedFee(),
                bill.getGatewayFee(),
                bill.getTaxRate(),
                bill.getFeeSettlementMode()));
    bill.setTransactionFee(result.transactionFee());
    bill.setTaxFee(result.taxFee());
    bill.setTotalFee(result.totalFee());
    bill.setSettlementAmount(result.settlementAmount());
  }

  private void markFailed(SubscriptionBillingRecord bill, String reason) {
    int retryCount = bill.getRetryCount() == null ? 0 : bill.getRetryCount();
    bill.setRetryCount(retryCount + 1);
    bill.setFailureReason(reason);
    bill.setStatus(SubscriptionBillingStatus.FAILED);
    if (bill.getRetryCount() < properties.getSubscription().getMaxRetryCount()) {
      bill.setNextRetryAt(LocalDateTime.now().plusSeconds(properties.getSubscription().getRetryBackoffSeconds()));
    } else {
      bill.setNextRetryAt(null);
    }
    saveBillStatusChange(
        bill,
        Set.of(
            SubscriptionBillingStatus.PENDING,
            SubscriptionBillingStatus.EXECUTING,
            SubscriptionBillingStatus.FAILED));
    auditService.record("SUBSCRIPTION_BILLING_FAILED", "SUBSCRIPTION_BILLING",
        bill.getId().toString(), "FAILED", bill);
    log.warn("订阅账单执行失败。subscriptionOrderNo={}, billingSequence={}, retryCount={}, reason={}",
        bill.getSubscriptionOrderNo(), bill.getBillingSequence(), bill.getRetryCount(), reason);
  }

  private void saveBillStatusChange(
      SubscriptionBillingRecord bill, Set<SubscriptionBillingStatus> allowedStatuses) {
    if (!billingRepository.saveIfStatusIn(bill, allowedStatuses)) {
      throw new IllegalStateException("Subscription billing status changed concurrently: " + bill.getId());
    }
  }
}
