package io.swzxsyh.payment.application;

import io.swzxsyh.payment.callback.PaymentCallbackDeliveryService;
import io.swzxsyh.payment.accounting.MerchantBalanceLedgerService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.exceptionorder.PaymentExceptionOrderService;
import io.swzxsyh.payment.kyt.KytDecision;
import io.swzxsyh.payment.kyt.KytScreeningRequest;
import io.swzxsyh.payment.kyt.KytScreeningResult;
import io.swzxsyh.payment.kyt.KytScreeningService;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventSubscriber;
import io.swzxsyh.payment.pending.PendingChainTransactionService;
import io.swzxsyh.payment.rawchain.RawChainLogService;
import io.swzxsyh.payment.routing.WalletAccountType;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordService;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.watcher.dto.PaymentNotificationRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * payment 端链上入账事件应用服务。
 *
 * <p>扫描器只发布链上事实，本服务独占消费并编排 core 的订单认账、合约隔离记录和商户回调。
 */
@Slf4j
@Service
@ConditionalOnProperty(
    prefix = "crypto.payment.messaging",
    name = "consumer-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ChainPaymentEventApplicationService {

  private final CryptoPaymentProperties properties;
  private final ChainPaymentEventSubscriber eventSubscriber;
  private final PaymentOrderService paymentOrderService;
  private final ContractSettlementRecordService settlementRecordService;
  private final PaymentCallbackDeliveryService callbackDeliveryService;
  private final RawChainLogService rawChainLogService;
  private final PendingChainTransactionService pendingTransactionService;
  private final KytScreeningService kytScreeningService;
  private final PaymentExceptionOrderService exceptionOrderService;
  private final MerchantBalanceLedgerService merchantBalanceLedgerService;
  private final LockUtil lockUtil;
  private ChainPaymentEventSubscriber.Subscription subscription;

  public ChainPaymentEventApplicationService(
      CryptoPaymentProperties properties,
      ChainPaymentEventSubscriber eventSubscriber,
      PaymentOrderService paymentOrderService,
      ContractSettlementRecordService settlementRecordService,
      PaymentCallbackDeliveryService callbackDeliveryService,
      RawChainLogService rawChainLogService,
      PendingChainTransactionService pendingTransactionService,
      KytScreeningService kytScreeningService,
      PaymentExceptionOrderService exceptionOrderService,
      MerchantBalanceLedgerService merchantBalanceLedgerService,
      LockUtil lockUtil) {
    this.properties = properties;
    this.eventSubscriber = eventSubscriber;
    this.paymentOrderService = paymentOrderService;
    this.settlementRecordService = settlementRecordService;
    this.callbackDeliveryService = callbackDeliveryService;
    this.rawChainLogService = rawChainLogService;
    this.pendingTransactionService = pendingTransactionService;
    this.kytScreeningService = kytScreeningService;
    this.exceptionOrderService = exceptionOrderService;
    this.merchantBalanceLedgerService = merchantBalanceLedgerService;
    this.lockUtil = lockUtil;
  }

  /** 启动链上入账事件订阅，底层可以是 Redis Pub/Sub，也可以替换为真实 MQ。 */
  @PostConstruct
  public void subscribe() {
    subscription = eventSubscriber.subscribe(this::handle);
    log.info("payment 端链上入账消息消费者已启动。provider={}, topic={}",
        properties.getMessaging().getProvider(), properties.getMessaging().getChainPaymentTopic());
  }

  /** 应用关闭时注销监听，避免本地热重启产生重复 listener。 */
  @PreDestroy
  public void unsubscribe() {
    if (subscription == null) {
      return;
    }
    subscription.close();
    log.info("payment 端链上入账消息消费者已停止。provider={}, topic={}",
        properties.getMessaging().getProvider(), properties.getMessaging().getChainPaymentTopic());
  }

  /** 消费链上入账事件，并统一推进订单支付闭环。 */
  public void handle(ChainPaymentEvent event) {
    if (event == null || !StringUtils.hasText(event.txHash())) {
      log.debug("链上入账消息为空或缺少 txHash，跳过。event={}", event);
      return;
    }
    log.info("开始消费链上入账消息。eventId={}, source={}, chain={}, txHash={}, to={}, amount={}, tokenAddress={}",
        event.eventId(),
        event.source(),
        event.chain(),
        event.txHash(),
        event.destinationAddress(),
        event.amount(),
        event.tokenAddress());

    String lockKey = RedisKeyNamespace.chainPaymentEventLock(properties, event.chain(), event.txHash());
    try {
      lockUtil.withLock(lockKey, 200, 30, () -> handleLocked(event));
    } catch (IllegalStateException ex) {
      if (ex.getMessage() != null && ex.getMessage().startsWith("Failed to acquire lock")) {
        log.info("链上入账消息正在被其它 payment 节点处理，本节点跳过。eventId={}, chain={}, txHash={}",
            event.eventId(), event.chain(), event.txHash());
        return;
      }
      throw ex;
    }
  }

  private void handleLocked(ChainPaymentEvent event) {
    rawChainLogService.recordObserved(event);
    Optional<PaymentOrder> matchedOrder =
        paymentOrderService.recordChainPayment(
            event.chain(),
            event.destinationAddress(),
            event.txHash(),
            event.amount(),
            StringUtils.hasText(event.tokenAddress()) ? event.tokenAddress() : null,
            event.sourceAddress(),
            event.blockNumber());
    if (matchedOrder.isEmpty()) {
      rawChainLogService.markUnmatched(event, "未匹配到支付订单或被交易幂等拦截");
      log.debug("链上入账消息未匹配订单或已被幂等拦截。eventId={}, chain={}, txHash={}, to={}",
          event.eventId(), event.chain(), event.txHash(), event.destinationAddress());
      return;
    }

    PaymentOrder order = matchedOrder.get();
    pendingTransactionService.markMatched(event, order.getCryptoOrderNo());
    rawChainLogService.markMatched(event, order.getCryptoOrderNo());
    boolean contractAwaitingRelease = isContractAwaitingRelease(order);
    if (contractAwaitingRelease) {
      settlementRecordService.markIsolated(order.getCryptoOrderNo(), event.txHash(), event.blockNumber());
      log.info("合约收款已进入隔离态，后置 KYT 通过后仍需等待合约放行事件或人工放行。eventId={}, cryptoOrderNo={}, txHash={}, blockNumber={}",
          event.eventId(), order.getCryptoOrderNo(), event.txHash(), event.blockNumber());
    }

    KytScreeningResult kytResult = runPostPaymentKyt(event, order);
    if (requiresManualKytHandling(kytResult)) {
      exceptionOrderService.createKytPaymentException(
          order,
          event.chain(),
          event.sourceAddress(),
          event.destinationAddress(),
          event.txHash(),
          event.amount(),
          event.tokenAddress(),
          event.blockNumber(),
          kytResult);
      if (!order.isLatePayment()) {
        paymentOrderService.markKytReview(order.getCryptoOrderNo(), buildKytReason(kytResult));
      }
      log.warn(
          "链上入账命中后置 KYT，已拦截商户回调并进入人工处理。eventId={}, cryptoOrderNo={}, decision={}, riskScore={}, txHash={}, to={}",
          event.eventId(),
          order.getCryptoOrderNo(),
          kytResult.decision(),
          kytResult.riskScore(),
          event.txHash(),
          event.destinationAddress());
      return;
    }
    if (order.isLatePayment()) {
      log.warn("延迟到账已记录，等待人工确认，不自动回调商户。eventId={}, cryptoOrderNo={}, merchantOrderNo={}, txHash={}, realAmount={}, status={}",
          event.eventId(),
          order.getCryptoOrderNo(),
          order.getMerchantOrderNo(),
          event.txHash(),
          event.amount(),
          order.getStatus());
      return;
    }

    if (contractAwaitingRelease) {
      log.info("合约收款 KYT 已通过，订单保持 CONFIRMING，等待最终放行后再入账和回调。eventId={}, cryptoOrderNo={}, txHash={}",
          event.eventId(), order.getCryptoOrderNo(), event.txHash());
      return;
    }

    merchantBalanceLedgerService.creditPaymentOrder(order);

    PaymentNotificationRecord notification =
        new PaymentNotificationRecord(
            event.chain(),
            order.getCryptoOrderNo(),
            order.getMerchantId(),
            order.getMerchantOrderNo(),
            event.txHash(),
            event.sourceAddress(),
            event.destinationAddress(),
            order.getAmount() == null ? event.amount() : order.getAmount(),
            event.amount(),
            event.blockNumber(),
            order.getTransactionFee(),
            order.getTaxFee(),
            order.getTotalFee(),
            order.getSettlementAmount(),
            order.isLatePayment(),
            order.getStatus().name());
    handlePaymentSuccess(order.getNotifyUrl(), notification);
  }

  private boolean isContractAwaitingRelease(PaymentOrder order) {
    return order != null
        && order.getPaymentMethod() == PaymentMethod.CONTRACT
        && order.getStatus() == OrderStatus.CONFIRMING;
  }

  /**
   * 链上入账后的 KYT 复核。
   *
   * <p>地址转账模式无法在付款前可靠拿到真实付款方地址，所以必须在交易被确认并解析出
   * from/to/amount 后再做一次 KYT。KYT 未开启时会返回 APPROVE，不改变现有支付流程。
   */
  private KytScreeningResult runPostPaymentKyt(ChainPaymentEvent event, PaymentOrder order) {
    KytScreeningResult result;
    try {
      result =
          kytScreeningService.screen(new KytScreeningRequest(
              event.chain(),
              order.getToken(),
              StringUtils.hasText(event.tokenAddress()) ? event.tokenAddress() : order.getTokenAddress(),
              order.getMerchantId(),
              order.getCryptoOrderNo(),
              event.sourceAddress(),
              event.destinationAddress(),
              resolveWalletAccountType(order),
              event.amount()));
    } catch (Exception ex) {
      if (!properties.getKyt().isEnabled() || !properties.getKyt().isStrictMode()) {
        log.warn("后置 KYT 执行异常，当前为非严格模式，继续放行。eventId={}, cryptoOrderNo={}, error={}",
            event.eventId(), order.getCryptoOrderNo(), ex.getMessage(), ex);
        return new KytScreeningResult(
            properties.getKyt().isEnabled(),
            KytDecision.APPROVE,
            0,
            "kyt-error-passthrough",
            List.of(),
            List.of("kyt provider error: " + ex.getMessage()),
            LocalDateTime.now());
      }
      log.error("后置 KYT 执行异常，严格模式下进入人工复核。eventId={}, cryptoOrderNo={}, error={}",
          event.eventId(), order.getCryptoOrderNo(), ex.getMessage(), ex);
      return new KytScreeningResult(
          true,
          KytDecision.REVIEW,
          properties.getKyt().getReviewThreshold(),
          "kyt-error-strict",
          List.of(),
          List.of("kyt provider error: " + ex.getMessage()),
          LocalDateTime.now());
    }
    log.info("后置 KYT 完成。eventId={}, cryptoOrderNo={}, enabled={}, decision={}, riskScore={}",
        event.eventId(),
        order.getCryptoOrderNo(),
        result.enabled(),
        result.decision(),
        result.riskScore());
    return result;
  }

  private boolean requiresManualKytHandling(KytScreeningResult result) {
    return result != null
        && result.enabled()
        && (result.decision() == KytDecision.REJECT || result.decision() == KytDecision.REVIEW);
  }

  private WalletAccountType resolveWalletAccountType(PaymentOrder order) {
    if (order == null || !StringUtils.hasText(order.getWalletAccountType())) {
      return WalletAccountType.UNKNOWN;
    }
    try {
      return WalletAccountType.valueOf(order.getWalletAccountType());
    } catch (IllegalArgumentException ex) {
      return WalletAccountType.UNKNOWN;
    }
  }

  private String buildKytReason(KytScreeningResult result) {
    if (result == null) {
      return "KYT result missing";
    }
    String reasons = result.reasons() == null || result.reasons().isEmpty()
        ? "no detail reasons"
        : String.join("; ", result.reasons());
    return "decision=" + result.decision()
        + ", provider=" + result.selectedProvider()
        + ", riskScore=" + result.riskScore()
        + ", reasons=" + reasons;
  }

  private void handlePaymentSuccess(String notifyUrl, PaymentNotificationRecord notification) {
    try {
      if (!StringUtils.hasText(notifyUrl)) {
        log.warn("回调地址为空，跳过通知。chain={}, txHash={}", notification.chain(), notification.txHash());
        return;
      }
      callbackDeliveryService.enqueueAndDispatch(
          resolvePaymentCallbackEvent(notification.orderStatus()),
          notification.merchantId(),
          notifyUrl,
          notification);
      log.info("支付回调已入队。cryptoOrderNo={}, merchantId={}, txHash={}, status={}",
          notification.cryptoOrderNo(),
          notification.merchantId(),
          notification.txHash(),
          notification.orderStatus());
    } catch (Exception e) {
      log.error("支付回调入队失败。txHash={}, error={}", notification.txHash(), e.getMessage(), e);
    }
  }

  private String resolvePaymentCallbackEvent(String orderStatus) {
    if (OrderStatus.UNDERPAID.name().equals(orderStatus)) {
      return "PAYMENT_UNDERPAID";
    }
    if (OrderStatus.OVERPAID.name().equals(orderStatus)) {
      return "PAYMENT_OVERPAID";
    }
    if (OrderStatus.CONFIRMING.name().equals(orderStatus)) {
      return "PAYMENT_CONFIRMING";
    }
    return "PAYMENT_SUCCESS";
  }
}
