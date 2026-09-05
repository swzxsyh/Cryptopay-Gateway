package io.swzxsyh.payment.subscription;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeSnapshot;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.callback.PaymentCallbackDeliveryService;
import io.swzxsyh.payment.idempotency.DbIdempotencyService;
import io.swzxsyh.payment.merchant.MerchantPaymentChannelPolicyService;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import io.swzxsyh.payment.security.UrlSafetyService;
import io.swzxsyh.payment.signature.CashierTokenService;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderResponse;
import io.swzxsyh.payment.subscription.dto.SubscriptionActionResponse;
import io.swzxsyh.payment.subscription.dto.SubscriptionBillingRecordView;
import io.swzxsyh.payment.subscription.dto.SubscriptionCashierBootstrap;
import io.swzxsyh.payment.subscription.dto.SubscriptionOrderView;
import io.swzxsyh.payment.subscription.dto.SubscriptionStatusNotificationRecord;
import io.swzxsyh.payment.subscription.model.SubscriptionSetupPlan;
import io.swzxsyh.payment.subscription.contract.SubscriptionContractAdapterRegistry;
import io.swzxsyh.payment.subscription.contract.SubscriptionExecutionResult;
import io.swzxsyh.payment.subscription.strategy.ConfiguredSubscriptionStrategy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
/** 订阅订单的核心编排服务。 */
public class SubscriptionOrderService {

  private static final DateTimeFormatter ORDER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final SubscriptionOrderRepository orderRepository;
  private final CryptoPaymentProperties properties;
  private final ConfiguredSubscriptionStrategy strategy;
  private final DbIdempotencyService idempotencyService;
  private final UrlSafetyService urlSafetyService;
  private final PaymentAuditService auditService;
  private final SubscriptionBillingRepository billingRepository;
  private final SubscriptionStateMachine stateMachine;
  private final SubscriptionContractAdapterRegistry adapterRegistry;
  private final MerchantPaymentChannelPolicyService merchantChannelPolicyService;
  private final PaymentCallbackDeliveryService callbackDeliveryService;
  private final CashierTokenService cashierTokenService;
  private final SubscriptionKytGuard subscriptionKytGuard;

  public SubscriptionOrderService(
      SubscriptionOrderRepository orderRepository,
      CryptoPaymentProperties properties,
      ConfiguredSubscriptionStrategy strategy,
      DbIdempotencyService idempotencyService,
      UrlSafetyService urlSafetyService,
      PaymentAuditService auditService,
      SubscriptionBillingRepository billingRepository,
      SubscriptionStateMachine stateMachine,
      SubscriptionContractAdapterRegistry adapterRegistry,
      MerchantPaymentChannelPolicyService merchantChannelPolicyService,
      PaymentCallbackDeliveryService callbackDeliveryService,
      CashierTokenService cashierTokenService,
      SubscriptionKytGuard subscriptionKytGuard) {
    this.orderRepository = orderRepository;
    this.properties = properties;
    this.strategy = strategy;
    this.idempotencyService = idempotencyService;
    this.urlSafetyService = urlSafetyService;
    this.auditService = auditService;
    this.billingRepository = billingRepository;
    this.stateMachine = stateMachine;
    this.adapterRegistry = adapterRegistry;
    this.merchantChannelPolicyService = merchantChannelPolicyService;
    this.callbackDeliveryService = callbackDeliveryService;
    this.cashierTokenService = cashierTokenService;
    this.subscriptionKytGuard = subscriptionKytGuard;
  }

  /** 创建订阅订单。 */
  public CreateSubscriptionOrderResponse createOrder(CreateSubscriptionOrderRequest request) {
    String merchantId = StringUtils.hasText(request.merchantId()) ? request.merchantId().trim() : "";
    return idempotencyService.executeOnceRequired(
        "subscription-order:" + merchantId,
        request.idempotencyKey(),
        CreateSubscriptionOrderResponse.class,
        () -> createOrderInternal(request));
  }

  private CreateSubscriptionOrderResponse createOrderInternal(CreateSubscriptionOrderRequest request) {
    validate(request);

    LocalDateTime now = LocalDateTime.now();
    SubscriptionOrder order = new SubscriptionOrder();
    order.setSubscriptionOrderNo(generateSubscriptionOrderNo(now));
    order.setMerchantOrderNo(request.merchantOrderNo());
    order.setMerchantId(request.merchantId());
    order.setAmountPerCycle(request.amountPerCycle());
    order.setCurrency(StringUtils.hasText(request.currency()) ? request.currency() : "USDT");
    order.setChain(request.chain());
    order.setToken(request.token());
    order.setTokenAddress(request.tokenAddress());
    order.setPayerAddress(request.payerAddress());
    order.setRecipientAddress(request.recipientAddress());
    order.setBillingMode(resolveBillingMode(request.billingMode()));
    order.setCycleSeconds(request.cycleSeconds() == null ? properties.getSubscription().getDefaultCycleSeconds() : request.cycleSeconds());
    MerchantPaymentChannelConfig channelConfig = merchantChannelPolicyService.usableChannel(
        request.merchantId(), request.chain(), request.token(), request.amountPerCycle());
    order.setNotifyUrl(request.notifyUrl());
    order.setReturnUrl(request.returnUrl());
    order.setStatus(SubscriptionStatus.CREATED);
    order.setCreatedAt(now);
    order.setUpdatedAt(now);

    SubscriptionSetupPlan setupPlan = strategy.prepare(order, request);
    order.setBillingMode(setupPlan.billingMode());
    applyFeeSnapshot(order, MerchantSettlementFeeSnapshot.from(channelConfig));
    order.setNextBillingAt(now.plusSeconds(nextBillingDelaySeconds(order)));
    order.setSetupContractAddress(setupPlan.setupContractAddress());
    order.setSetupPayload(setupPlan.setupPayload());
    order.setSetupReason(setupPlan.setupReason());
    order.setSubscriptionEventId(SubscriptionEventId.fromOrderNo(order.getSubscriptionOrderNo()));

    // 订阅合约参数下发前先做 KYT 预检查；KYT 关闭时 guard 会放行，不影响未启用风控的流程。
    subscriptionKytGuard.precheckSetup(order);
    orderRepository.save(order);
    String subscriptionToken = buildSubscriptionToken(order);
    String subscriptionUrl = buildSubscriptionUrl(subscriptionToken);
    auditService.record("SUBSCRIPTION_ORDER_CREATED", "SUBSCRIPTION_ORDER", order.getSubscriptionOrderNo(), "CREATED", order);
    log.info("Created subscription order. subscriptionOrderNo={}, merchantOrderNo={}, billingMode={}, subscriptionUrl={}",
        order.getSubscriptionOrderNo(), order.getMerchantOrderNo(), order.getBillingMode(), subscriptionUrl);
    return new CreateSubscriptionOrderResponse(
        order.getSubscriptionOrderNo(),
        subscriptionToken,
        subscriptionUrl,
        SubscriptionOrderView.from(order)
    );
  }

  /** 根据订阅单号查询订单。 */
  public SubscriptionOrder getOrder(String subscriptionOrderNo) {
    return orderRepository.findBySubscriptionOrderNo(subscriptionOrderNo)
        .orElseThrow(() -> new IllegalArgumentException("Subscription order not found: " + subscriptionOrderNo));
  }

  /** 查询订阅收银台启动数据。 */
  public SubscriptionCashierBootstrap getCashierBootstrap(String subscriptionOrderNo) {
    SubscriptionOrder order = getOrder(subscriptionOrderNo);
    SubscriptionBillingRecordView latestBilling =
        billingRepository.findLatestBySubscriptionOrderNo(subscriptionOrderNo)
            .map(SubscriptionBillingRecordView::from)
            .orElse(null);
    return new SubscriptionCashierBootstrap(
        SubscriptionOrderView.from(order),
        latestBilling,
        order.getBillingMode().name(),
        order.getSetupContractAddress(),
        order.getSetupPayload(),
        order.getSetupReason());
  }

  /** 通过订阅收银台 token 解析单号并返回启动数据，公开页面不再信任 URL 中的明文订阅单号。 */
  public SubscriptionCashierBootstrap getCashierBootstrapByToken(String subscriptionToken) {
    return getCashierBootstrap(cashierTokenService.resolveSubscriptionOrderNo(subscriptionToken));
  }

  /** 订阅收银台前端通过 token 上报初始化交易，避免把订阅单号作为用户侧接口参数。 */
  public SubscriptionActionResponse submitSetupTxByToken(String subscriptionToken, String txHash) {
    return submitSetupTx(cashierTokenService.resolveSubscriptionOrderNo(subscriptionToken), txHash);
  }

  /** 前端钱包完成初始化交易后，上报 txHash，后端等待链上确认。 */
  public SubscriptionActionResponse submitSetupTx(String subscriptionOrderNo, String txHash) {
    if (!StringUtils.hasText(txHash)) {
      throw new IllegalArgumentException("txHash is required");
    }
    SubscriptionOrder order = getOrder(subscriptionOrderNo);
    order.setSetupTxHash(txHash.trim());
    SubscriptionStatus previousStatus = order.getStatus();
    order.setStatus(stateMachine.transition(previousStatus, SubscriptionStatus.ACTIVATING));
    order.setFailureReason(null);
    saveStatusChange(order, Set.of(SubscriptionStatus.CREATED, SubscriptionStatus.ACTIVATING));
    auditService.record("SUBSCRIPTION_SETUP_TX_SUBMITTED", "SUBSCRIPTION_ORDER",
        order.getSubscriptionOrderNo(), order.getStatus().name(), order);
    log.info("订阅初始化交易已上报。subscriptionOrderNo={}, txHash={}",
        order.getSubscriptionOrderNo(), txHash);
    return new SubscriptionActionResponse(
        order.getSubscriptionOrderNo(), order.getStatus().name(), false, txHash, "setup transaction recorded");
  }

  /** 订阅初始化交易确认后激活订阅。 */
  public SubscriptionOrder activate(String subscriptionOrderNo) {
    SubscriptionOrder order = getOrder(subscriptionOrderNo);
    if (order.getStatus() == SubscriptionStatus.ACTIVE) {
      return order;
    }
    LocalDateTime now = LocalDateTime.now();
    SubscriptionStatus previousStatus = order.getStatus();
    order.setStatus(stateMachine.transition(previousStatus, SubscriptionStatus.ACTIVE));
    order.setActivatedAt(now);
    order.setFailureReason(null);
    if (order.getNextBillingAt() == null || order.getNextBillingAt().isBefore(now)) {
      order.setNextBillingAt(now.plusSeconds(nextBillingDelaySeconds(order)));
    }
    saveStatusChange(order, Set.of(SubscriptionStatus.CREATED, SubscriptionStatus.ACTIVATING, SubscriptionStatus.FAILED));
    auditService.record("SUBSCRIPTION_ACTIVATED", "SUBSCRIPTION_ORDER",
        order.getSubscriptionOrderNo(), "ACTIVE", order);
    log.info("订阅已激活。subscriptionOrderNo={}, nextBillingAt={}",
        order.getSubscriptionOrderNo(), order.getNextBillingAt());
    return order;
  }

  /** 根据初始化交易哈希确认订阅激活。 */
  public boolean activateBySetupTxHash(String txHash) {
    return orderRepository.findBySetupTxHash(txHash)
        .map(order -> {
          activate(order.getSubscriptionOrderNo());
          return true;
        })
        .orElse(false);
  }

  /** 根据链上订阅事件 ID 激活订阅，供合约事件扫描使用。 */
  public boolean activateByEventId(String subscriptionEventId) {
    return orderRepository.findBySubscriptionEventId(subscriptionEventId)
        .map(order -> {
          activate(order.getSubscriptionOrderNo());
          return true;
        })
        .orElse(false);
  }

  /** 根据链上订阅事件 ID 暂停订阅，事件来自链上时不再重复发起合约交易。 */
  public boolean pauseByEventId(String subscriptionEventId) {
    return applyChainStatusEvent(
        subscriptionEventId,
        SubscriptionStatus.PAUSED,
        Set.of(SubscriptionStatus.ACTIVE),
        "SUBSCRIPTION_PAUSED_ON_CHAIN",
        "链上事件确认订阅已暂停。");
  }

  /** 根据链上订阅事件 ID 恢复订阅，事件来自链上时只更新本地状态。 */
  public boolean resumeByEventId(String subscriptionEventId) {
    return applyChainStatusEvent(
        subscriptionEventId,
        SubscriptionStatus.ACTIVE,
        Set.of(SubscriptionStatus.PAUSED, SubscriptionStatus.FAILED),
        "SUBSCRIPTION_RESUMED_ON_CHAIN",
        "链上事件确认订阅已恢复。");
  }

  /** 根据链上订阅事件 ID 取消订阅，事件来自链上时只更新本地状态。 */
  public boolean cancelByEventId(String subscriptionEventId) {
    return applyChainStatusEvent(
        subscriptionEventId,
        SubscriptionStatus.CANCELLED,
        Set.of(
            SubscriptionStatus.CREATED,
            SubscriptionStatus.ACTIVATING,
            SubscriptionStatus.ACTIVE,
            SubscriptionStatus.PAUSED,
            SubscriptionStatus.FAILED),
        "SUBSCRIPTION_CANCELLED_ON_CHAIN",
        "链上事件确认订阅已取消。");
  }

  /** 暂停订阅。 */
  public SubscriptionActionResponse pause(String subscriptionOrderNo) {
    SubscriptionOrder order = getOrder(subscriptionOrderNo);
    SubscriptionExecutionResult result = adapterRegistry.get(order.getBillingMode()).pause(order);
    SubscriptionStatus previousStatus = order.getStatus();
    order.setStatus(stateMachine.transition(previousStatus, SubscriptionStatus.PAUSED));
    order.setPausedAt(LocalDateTime.now());
    saveStatusChange(order, Set.of(SubscriptionStatus.ACTIVE));
    auditService.record("SUBSCRIPTION_PAUSED", "SUBSCRIPTION_ORDER",
        order.getSubscriptionOrderNo(), order.getStatus().name(), order);
    enqueueStatusCallback(
        order, "SUBSCRIPTION_PAUSED", previousStatus, order.getStatus(), result.txHash(), result.message());
    return new SubscriptionActionResponse(
        order.getSubscriptionOrderNo(), order.getStatus().name(), result.submitted(), result.txHash(), result.message());
  }

  /** 恢复订阅。 */
  public SubscriptionActionResponse resume(String subscriptionOrderNo) {
    SubscriptionOrder order = getOrder(subscriptionOrderNo);
    SubscriptionExecutionResult result = adapterRegistry.get(order.getBillingMode()).resume(order);
    SubscriptionStatus previousStatus = order.getStatus();
    order.setStatus(stateMachine.transition(previousStatus, SubscriptionStatus.ACTIVE));
    order.setPausedAt(null);
    order.setFailureReason(null);
    saveStatusChange(order, Set.of(SubscriptionStatus.PAUSED, SubscriptionStatus.FAILED));
    auditService.record("SUBSCRIPTION_RESUMED", "SUBSCRIPTION_ORDER",
        order.getSubscriptionOrderNo(), order.getStatus().name(), order);
    enqueueStatusCallback(
        order, "SUBSCRIPTION_RESUMED", previousStatus, order.getStatus(), result.txHash(), result.message());
    return new SubscriptionActionResponse(
        order.getSubscriptionOrderNo(), order.getStatus().name(), result.submitted(), result.txHash(), result.message());
  }

  /** 取消订阅。 */
  public SubscriptionActionResponse cancel(String subscriptionOrderNo) {
    SubscriptionOrder order = getOrder(subscriptionOrderNo);
    SubscriptionExecutionResult result = adapterRegistry.get(order.getBillingMode()).cancel(order);
    SubscriptionStatus previousStatus = order.getStatus();
    order.setStatus(stateMachine.transition(previousStatus, SubscriptionStatus.CANCELLED));
    order.setCancelledAt(LocalDateTime.now());
    saveStatusChange(order, Set.of(
        SubscriptionStatus.CREATED,
        SubscriptionStatus.ACTIVATING,
        SubscriptionStatus.ACTIVE,
        SubscriptionStatus.PAUSED,
        SubscriptionStatus.FAILED));
    auditService.record("SUBSCRIPTION_CANCELLED", "SUBSCRIPTION_ORDER",
        order.getSubscriptionOrderNo(), order.getStatus().name(), order);
    enqueueStatusCallback(
        order, "SUBSCRIPTION_CANCELLED", previousStatus, order.getStatus(), result.txHash(), result.message());
    return new SubscriptionActionResponse(
        order.getSubscriptionOrderNo(), order.getStatus().name(), result.submitted(), result.txHash(), result.message());
  }

  private boolean applyChainStatusEvent(
      String subscriptionEventId,
      SubscriptionStatus targetStatus,
      Set<SubscriptionStatus> allowedStatuses,
      String auditEvent,
      String message) {
    return orderRepository.findBySubscriptionEventId(subscriptionEventId)
        .map(order -> {
          if (order.getStatus() == targetStatus) {
            return true;
          }
          LocalDateTime now = LocalDateTime.now();
          SubscriptionStatus previousStatus = order.getStatus();
          order.setStatus(stateMachine.transition(previousStatus, targetStatus));
          order.setFailureReason(null);
          if (targetStatus == SubscriptionStatus.PAUSED) {
            order.setPausedAt(now);
          } else if (targetStatus == SubscriptionStatus.ACTIVE) {
            order.setPausedAt(null);
          } else if (targetStatus == SubscriptionStatus.CANCELLED) {
            order.setCancelledAt(now);
          }
          saveStatusChange(order, allowedStatuses);
          auditService.record(auditEvent, "SUBSCRIPTION_ORDER",
              order.getSubscriptionOrderNo(), targetStatus.name(), order);
          enqueueStatusCallback(order, auditEvent, previousStatus, targetStatus, null, message);
          log.info("{} subscriptionOrderNo={}, eventId={}, previousStatus={}, currentStatus={}",
              message, order.getSubscriptionOrderNo(), SubscriptionEventId.normalize(subscriptionEventId),
              previousStatus, targetStatus);
          return true;
        })
        .orElse(false);
  }

  private void enqueueStatusCallback(
      SubscriptionOrder order,
      String eventType,
      SubscriptionStatus previousStatus,
      SubscriptionStatus currentStatus,
      String txHash,
      String reason) {
    if (!StringUtils.hasText(order.getNotifyUrl())) {
      return;
    }
    SubscriptionStatusNotificationRecord notification =
        new SubscriptionStatusNotificationRecord(
            order.getSubscriptionOrderNo(),
            order.getMerchantOrderNo(),
            order.getMerchantId(),
            order.getChain(),
            order.getToken(),
            eventType,
            previousStatus == null ? null : previousStatus.name(),
            currentStatus == null ? null : currentStatus.name(),
            order.getBillingMode(),
            txHash,
            reason);
    callbackDeliveryService.enqueuePayloadAndDispatch(
        eventType,
        order.getMerchantId(),
        order.getChain(),
        order.getSubscriptionOrderNo(),
        order.getMerchantOrderNo(),
        order.getNotifyUrl(),
        notification);
  }

  private void validate(CreateSubscriptionOrderRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request is required");
    }
    if (!StringUtils.hasText(request.merchantOrderNo())) {
      throw new IllegalArgumentException("merchantOrderNo is required");
    }
    if (!StringUtils.hasText(request.merchantId())) {
      throw new IllegalArgumentException("merchantId is required");
    }
    if (request.amountPerCycle() == null || request.amountPerCycle().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("amountPerCycle must be greater than zero");
    }
    urlSafetyService.validateMerchantCallbackUrl(request.notifyUrl(), "notifyUrl");
    urlSafetyService.validateMerchantReturnUrl(request.returnUrl(), "returnUrl");
    if (!StringUtils.hasText(request.chain())) {
      throw new IllegalArgumentException("chain is required");
    }
    if (!StringUtils.hasText(request.token())) {
      throw new IllegalArgumentException("token is required");
    }
    if (!StringUtils.hasText(request.tokenAddress())) {
      throw new IllegalArgumentException("tokenAddress is required");
    }
    if (!StringUtils.hasText(request.payerAddress())) {
      throw new IllegalArgumentException("payerAddress is required");
    }
    if (!StringUtils.hasText(request.recipientAddress())) {
      throw new IllegalArgumentException("recipientAddress is required");
    }
    if (!StringUtils.hasText(request.notifyUrl())) {
      throw new IllegalArgumentException("notifyUrl is required");
    }
    if (request.cycleSeconds() != null && request.cycleSeconds() < 1) {
      throw new IllegalArgumentException("cycleSeconds must be greater than or equal to 1");
    }
  }

  private SubscriptionBillingMode resolveBillingMode(SubscriptionBillingMode requested) {
    if (requested != null) {
      return requested;
    }
    String configured = properties.getSubscription().getDefaultMode();
    try {
      return SubscriptionBillingMode.valueOf(configured);
    } catch (Exception e) {
      return SubscriptionBillingMode.SUPERFLUID_STREAM;
    }
  }

  private String buildSubscriptionToken(SubscriptionOrder order) {
    Map<String, Object> attributes = new LinkedHashMap<>();
    attributes.put("cashierType", "SUBSCRIPTION");
    attributes.put("billingMode", order.getBillingMode() == null ? null : order.getBillingMode().name());
    attributes.put("subscriptionEventId", order.getSubscriptionEventId());
    return cashierTokenService.createSubscriptionToken(order, attributes);
  }

  private String buildSubscriptionUrl(String subscriptionToken) {
    return properties.getCashierBaseUrl() + "/subscription/" + subscriptionToken;
  }

  private void applyFeeSnapshot(SubscriptionOrder order, MerchantSettlementFeeSnapshot snapshot) {
    MerchantSettlementFeeSnapshot safeSnapshot =
        snapshot == null ? MerchantSettlementFeeSnapshot.zero() : snapshot;
    order.setTransactionFeeRate(safeSnapshot.transactionFeeRate());
    order.setMinimumFee(safeSnapshot.minimumFee());
    order.setFixedFee(safeSnapshot.fixedFee());
    order.setGatewayFee(safeSnapshot.gatewayFee());
    order.setTaxRate(safeSnapshot.taxRate());
    if (StringUtils.hasText(safeSnapshot.feeSettlementMode())) {
      order.setFeeSettlementMode(safeSnapshot.feeSettlementMode().trim().toUpperCase());
    } else if (order.getBillingMode() == SubscriptionBillingMode.SUPERFLUID_STREAM) {
      order.setFeeSettlementMode("PER_STREAM_WINDOW");
    } else {
      order.setFeeSettlementMode("PER_BILLING");
    }
  }

  private long nextBillingDelaySeconds(SubscriptionOrder order) {
    if (order.getBillingMode() == SubscriptionBillingMode.SUPERFLUID_STREAM) {
      return Math.max(1, properties.getSubscription().getStreamSettlementIntervalSeconds());
    }
    return Math.max(1, order.getCycleSeconds());
  }

  private void saveStatusChange(SubscriptionOrder order, Set<SubscriptionStatus> allowedStatuses) {
    if (!orderRepository.saveIfStatusIn(order, allowedStatuses)) {
      throw new IllegalStateException(
          "Subscription status changed concurrently: " + order.getSubscriptionOrderNo());
    }
  }

  private String generateSubscriptionOrderNo(LocalDateTime now) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    return "SB" + ORDER_TIME_FORMAT.format(now) + suffix;
  }
}
