package io.swzxsyh.payment.service;

import io.swzxsyh.payment.channel.PaymentChannel;
import io.swzxsyh.payment.channel.PaymentChannelFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeCalculator;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeResult;
import io.swzxsyh.payment.accounting.MerchantSettlementFeeSnapshot;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentDetails;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.channel.address.DerivedAddressInventoryService;
import io.swzxsyh.payment.exceptionorder.PaymentExceptionOrderService;
import io.swzxsyh.payment.idempotency.DbIdempotencyService;
import io.swzxsyh.payment.idempotency.DbReplayProtectionService;
import io.swzxsyh.payment.merchant.MerchantPaymentChannelPolicyService;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import io.swzxsyh.payment.scanner.ActiveDestinationAddressCache;
import io.swzxsyh.payment.exception.BizException;
import io.swzxsyh.payment.api.dto.ApiResponseCode;
import io.swzxsyh.payment.security.UrlSafetyService;
import io.swzxsyh.payment.signature.CashierTokenService;
import io.swzxsyh.payment.state.PaymentOrderStateMachine;
import io.swzxsyh.payment.routing.TokenCapabilityProfile;
import io.swzxsyh.payment.routing.TokenCapabilityResolver;
import io.swzxsyh.payment.routing.TokenRouteRequest;
import io.swzxsyh.payment.precheck.HostedWalletPrecheckService;
import io.swzxsyh.payment.channel.contract.ContractAddressPlanner;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

/** 普通支付订单的核心编排服务。 */
@Slf4j
@Service
public class PaymentOrderService {

  private static final DateTimeFormatter ORDER_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final PaymentOrderRepository orderRepository;
  private final PaymentChannelFactory channelFactory;
  private final CryptoPaymentProperties properties;
  private final DbIdempotencyService idempotencyService;
  private final DbReplayProtectionService replayProtectionService;
  private final CashierTokenService cashierTokenService;
  private final UrlSafetyService urlSafetyService;
  private final PaymentAuditService auditService;
  private final DerivedAddressInventoryService derivedAddressInventoryService;
  private final TokenCapabilityResolver tokenCapabilityResolver;
  private final HostedWalletPrecheckService hostedWalletPrecheckService;
  private final ContractAddressPlanner contractAddressPlanner;
  private final PaymentOrderStateMachine stateMachine;
  private final PaymentExceptionOrderService exceptionOrderService;
  private final MerchantPaymentChannelPolicyService merchantChannelPolicyService;
  private final MerchantSettlementFeeCalculator settlementFeeCalculator;
  private final ActiveDestinationAddressCache activeDestinationAddressCache;
  private final LockUtil lockUtil;
  private final HttpServletRequest request;

  public PaymentOrderService(
      PaymentOrderRepository orderRepository,
      PaymentChannelFactory channelFactory,
      CryptoPaymentProperties properties,
      DbIdempotencyService idempotencyService,
      DbReplayProtectionService replayProtectionService,
      CashierTokenService cashierTokenService,
      UrlSafetyService urlSafetyService,
      PaymentAuditService auditService,
      DerivedAddressInventoryService derivedAddressInventoryService,
      TokenCapabilityResolver tokenCapabilityResolver,
      HostedWalletPrecheckService hostedWalletPrecheckService,
      ContractAddressPlanner contractAddressPlanner,
      PaymentOrderStateMachine stateMachine,
      PaymentExceptionOrderService exceptionOrderService,
      MerchantPaymentChannelPolicyService merchantChannelPolicyService,
      MerchantSettlementFeeCalculator settlementFeeCalculator,
      ActiveDestinationAddressCache activeDestinationAddressCache,
      LockUtil lockUtil,
      HttpServletRequest request) {
    this.orderRepository = orderRepository;
    this.channelFactory = channelFactory;
    this.properties = properties;
    this.idempotencyService = idempotencyService;
    this.replayProtectionService = replayProtectionService;
    this.cashierTokenService = cashierTokenService;
    this.urlSafetyService = urlSafetyService;
    this.auditService = auditService;
    this.derivedAddressInventoryService = derivedAddressInventoryService;
    this.tokenCapabilityResolver = tokenCapabilityResolver;
    this.hostedWalletPrecheckService = hostedWalletPrecheckService;
    this.contractAddressPlanner = contractAddressPlanner;
    this.stateMachine = stateMachine;
    this.exceptionOrderService = exceptionOrderService;
    this.merchantChannelPolicyService = merchantChannelPolicyService;
    this.settlementFeeCalculator = settlementFeeCalculator;
    this.activeDestinationAddressCache = activeDestinationAddressCache;
    this.lockUtil = lockUtil;
    this.request = request;
  }

  /** 创建普通支付订单并支持幂等。 */
  public PaymentOrder createOrder(
      String merchantId,
      String merchantOrderNo,
      BigDecimal amount,
      String currency,
      String notifyUrl,
      String returnUrl,
      String idempotencyKey) {
    return idempotencyService.executeOnceRequired(
        "crypto-order:" + merchantId,
        idempotencyKey,
        PaymentOrder.class,
        () -> createOrderInternal(merchantId, merchantOrderNo, amount, currency, notifyUrl, returnUrl));
  }

  private PaymentOrder createOrderInternal(
      String merchantId,
      String merchantOrderNo,
      BigDecimal amount,
      String currency,
      String notifyUrl,
      String returnUrl) {
    if (!StringUtils.hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required");
    }
    if (!StringUtils.hasText(merchantOrderNo)) {
      throw new IllegalArgumentException("merchantOrderNo is required");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
    urlSafetyService.validateMerchantCallbackUrl(notifyUrl, "notifyUrl");
    urlSafetyService.validateMerchantReturnUrl(returnUrl, "returnUrl");

    LocalDateTime now = LocalDateTime.now();
    PaymentOrder order = new PaymentOrder();
    order.setCryptoOrderNo(generateCryptoOrderNo(now));
    order.setMerchantId(merchantId);
    order.setMerchantOrderNo(merchantOrderNo);
    order.setAmount(amount);
    order.setCurrency(StringUtils.hasText(currency) ? currency : "USDT");
    merchantChannelPolicyService.requireAnyUsable(merchantId, order.getCurrency(), amount);
    order.setNotifyUrl(notifyUrl);
    order.setReturnUrl(returnUrl);
    order.setStatus(OrderStatus.CREATED);
    order.setExpireTime(now.plusMinutes(properties.getOrderExpireMinutes()));
    order.setCreatedAt(now);
    order.setUpdatedAt(now);

    orderRepository.save(order);
    auditService.record(
        "ORDER_CREATED", "CRYPTO_ORDER", order.getCryptoOrderNo(), "CREATED", order);
    log.info(
        "Created crypto order. cryptoOrderNo={}, merchantId={}, merchantOrderNo={}, amount={}, currency={}, expireTime={}",
        order.getCryptoOrderNo(),
        merchantId,
        merchantOrderNo,
        amount,
        order.getCurrency(),
        order.getExpireTime());
    return order;
  }

  /** 根据用户选择的链、币种和钱包类型生成最终支付方式。 */
  @Transactional
  public PaymentOrder selectPaymentMethod(String cryptoOrderNo, PaymentSelection selection) {
    PaymentOrder order = getOrder(cryptoOrderNo);
    if (hasPaymentRoute(order)) {
      if (samePaymentRoute(order, selection)) {
        log.info("Payment method already selected, return existing route. cryptoOrderNo={}, method={}, chain={}, token={}",
            cryptoOrderNo, order.getPaymentMethod(), order.getChain(), order.getToken());
        return order;
      }
      throw new IllegalStateException(
          "Order payment route has already been confirmed and cannot be changed");
    }
    if (order.getStatus() == OrderStatus.PAID
        || order.getStatus() == OrderStatus.UNDERPAID
        || order.getStatus() == OrderStatus.OVERPAID
        || order.getStatus() == OrderStatus.CANCELLED
        || order.getStatus() == OrderStatus.EXPIRED) {
      throw new IllegalStateException(
          "Order status does not allow method selection: " + order.getStatus());
    }

    TokenCapabilityProfile tokenProfile =
        tokenCapabilityResolver
            .resolve(
                new TokenRouteRequest(
                    selection.chain(),
                    selection.token(),
                    selection.tokenAddress(),
                    selection.walletAddress(),
                    selection.walletAccountType()))
            .orElseThrow(() -> new IllegalArgumentException("Unable to resolve token capability"));

    if (StringUtils.hasText(order.getCurrency())
        && !order.getCurrency().equalsIgnoreCase(tokenProfile.token())) {
      throw new IllegalArgumentException("Selected token does not match order currency");
    }
    if (StringUtils.hasText(selection.tokenAddress())
        && !selection.tokenAddress().equalsIgnoreCase(tokenProfile.tokenAddress())) {
      throw new IllegalArgumentException(
          "Selected token address does not match resolved token profile");
    }
    MerchantPaymentChannelConfig channelConfig = merchantChannelPolicyService.usableChannel(
        order.getMerchantId(),
        tokenProfile.chain(),
        tokenProfile.token(),
        order.getAmount());
    applyFeeSnapshot(order, MerchantSettlementFeeSnapshot.from(channelConfig));

    PaymentSelection safeSelection =
        new PaymentSelection(
            selection.paymentMethod(),
            tokenProfile.chain(),
            tokenProfile.token(),
            tokenProfile.tokenAddress(),
            selection.walletAddress(),
            selection.walletAccountType());

    PaymentMethod method = selection.paymentMethod();
    PaymentChannel channel = channelFactory.get(method, safeSelection.chain(), safeSelection.token());

    if (method == PaymentMethod.CONTRACT) {
      // 托管钱包路线先做前置风控筛查，避免把合约调用参数提前下发给高风险订单。
      String payeeAddress = contractAddressPlanner.resolve(order, safeSelection).address();
      hostedWalletPrecheckService.precheck(
          order,
          safeSelection,
          tokenProfile,
          payeeAddress);
    }

    PaymentDetails details = channel.prepare(order, safeSelection);
    order.setPaymentMethod(details.paymentMethod());
    order.setChain(details.chain());
    order.setToken(details.token());
    order.setTokenAddress(details.tokenAddress());
    order.setWalletAddress(details.walletAddress());
    order.setWalletAccountType(
        details.walletAccountType() == null ? null : details.walletAccountType().name());
    order.setTokenRouteType(
        details.tokenRouteType() == null ? null : details.tokenRouteType().name());
    order.setRouteReason(details.routeReason());
    order.setPaymentAddress(details.paymentAddress());
    order.setContractAddress(details.contractAddress());
    order.setContractCallData(details.contractCallData());
    order.setGasPayerMode(details.gasPayerMode());
    order.setGasReason(details.gasReason());
    order.setGasEstimatedFeeWei(details.gasEstimatedFeeWei());
    order.setGasCustomerBalanceSufficient(details.gasCustomerBalanceSufficient());
    order.setGasPlatformBalanceSufficient(details.gasPlatformBalanceSufficient());
    order.setGasFallbackSuggestion(details.gasFallbackSuggestion());
    order.setDerivedAddressPoolKey(details.derivedAddressPoolKey());
    order.setDerivedAddressLeaseId(details.derivedAddressLeaseId());
    OrderStatus previousStatus = order.getStatus();
    order.setStatus(stateMachine.waitingPayment(previousStatus));
    saveOrderWithStatusGuard(
        order,
        selectableStatuses(),
        "PAYMENT_METHOD_SELECTED");
    auditService.record(
        "PAYMENT_METHOD_SELECTED",
        "CRYPTO_ORDER",
        order.getCryptoOrderNo(),
        "WAITING_PAYMENT",
        order);
    activeDestinationAddressCache.register(order);
    log.info(
        "Selected payment method. cryptoOrderNo={}, method={}, chain={}, token={}",
        cryptoOrderNo,
        method,
        safeSelection.chain(),
        safeSelection.token());
    return order;
  }

  private boolean hasPaymentRoute(PaymentOrder order) {
    return order.getPaymentMethod() != null
        || StringUtils.hasText(order.getPaymentAddress())
        || StringUtils.hasText(order.getContractAddress())
        || StringUtils.hasText(order.getContractCallData());
  }

  private boolean samePaymentRoute(PaymentOrder order, PaymentSelection selection) {
    return order.getPaymentMethod() == selection.paymentMethod()
        && sameText(order.getChain(), selection.chain())
        && sameText(order.getToken(), selection.token())
        && sameText(order.getTokenAddress(), selection.tokenAddress());
  }

  private boolean sameText(String left, String right) {
    String normalizedLeft = StringUtils.hasText(left) ? left.trim() : "";
    String normalizedRight = StringUtils.hasText(right) ? right.trim() : "";
    return normalizedLeft.equalsIgnoreCase(normalizedRight);
  }

  /** 记录前端钱包已广播的交易哈希，真正入账仍由扫链确认。 */
  @Transactional
  public PaymentOrder reportPaymentTxHash(String cryptoOrderNo, String txHash) {
    if (!StringUtils.hasText(txHash)) {
      throw new IllegalArgumentException("txHash is required");
    }
    PaymentOrder order = getOrder(cryptoOrderNo);
    if (order.getStatus() == OrderStatus.PAID) {
      return order;
    }
    if (order.getStatus() == OrderStatus.UNDERPAID || order.getStatus() == OrderStatus.OVERPAID) {
      return order;
    }
    if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
      throw new IllegalStateException("Order status does not allow tx hash report: " + order.getStatus());
    }
    if (order.getPaymentMethod() == null) {
      throw new IllegalStateException("Payment route must be confirmed before tx hash report");
    }

    String normalizedTxHash = txHash.trim();
    if (StringUtils.hasText(order.getPaymentTxHash())) {
      if (order.getPaymentTxHash().equalsIgnoreCase(normalizedTxHash)) {
        return order;
      }
      throw new IllegalStateException("Order already has a different txHash");
    }

    OrderStatus previousStatus = order.getStatus();
    order.setPaymentTxHash(normalizedTxHash);
    order.setStatus(stateMachine.detected(previousStatus));
    saveOrderWithStatusGuard(
        order,
        detectedStatuses(),
        "PAYMENT_TX_HASH_REPORTED");
    auditService.record(
        "PAYMENT_TX_HASH_REPORTED",
        "CRYPTO_ORDER",
        order.getCryptoOrderNo(),
        "DETECTED",
        order);
    log.info("Reported payment tx hash. cryptoOrderNo={}, chain={}, txHash={}",
        order.getCryptoOrderNo(), order.getChain(), normalizedTxHash);
    return order;
  }

  /** 根据到账地址和交易哈希记录链上支付。 */
  public Optional<PaymentOrder> recordChainPayment(String destinationAddress, String txHash) {
    return recordChainPayment(null, destinationAddress, txHash, null, null);
  }

  /** 根据到账地址、交易哈希和实际金额记录链上支付。 */
  public Optional<PaymentOrder> recordChainPayment(
      String destinationAddress, String txHash, BigDecimal amount) {
    return recordChainPayment(null, destinationAddress, txHash, amount, null);
  }

  /** 指定链记录链上支付。 */
  public Optional<PaymentOrder> recordChainPayment(
      String chain, String destinationAddress, String txHash) {
    return recordChainPayment(chain, destinationAddress, txHash, null, null);
  }

  /** 指定链与实际金额记录链上支付。 */
  public Optional<PaymentOrder> recordChainPayment(
      String chain, String destinationAddress, String txHash, BigDecimal amount) {
    return recordChainPayment(chain, destinationAddress, txHash, amount, null);
  }

  /** 指定链、实际金额与代币地址记录链上支付。 */
  public Optional<PaymentOrder> recordChainPayment(
      String chain, String destinationAddress, String txHash, BigDecimal amount, String tokenAddress) {
    return recordChainPayment(chain, destinationAddress, txHash, amount, tokenAddress, null, null);
  }

  /** 指定链、金额、代币地址、付款地址和区块号记录链上支付。 */
  public Optional<PaymentOrder> recordChainPayment(
      String chain,
      String destinationAddress,
      String txHash,
      BigDecimal amount,
      String tokenAddress,
      String sourceAddress,
      Long blockNumber) {
    LocalDateTime now = LocalDateTime.now();
    List<PaymentOrder> candidates =
        orderRepository
            .findByChainAndDestinationAddressAndTokenAddress(chain, destinationAddress, tokenAddress)
            .stream()
            .filter(order -> order.getStatus() != OrderStatus.PAID)
            .filter(order -> order.getStatus() != OrderStatus.UNDERPAID)
            .filter(order -> order.getStatus() != OrderStatus.OVERPAID)
            .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
            .toList();

    if (candidates.isEmpty()) {
      log.debug("链上支付未匹配到订单。chain={}, destinationAddress={}, tokenAddress={}, txHash={}",
          chain, destinationAddress, tokenAddress, txHash);
      return Optional.empty();
    }

    if (candidates.size() > 1) {
      log.warn("链上支付匹配到多个候选订单。chain={}, destinationAddress={}, tokenAddress={}, txHash={}, candidateCount={}",
          chain, destinationAddress, tokenAddress, txHash, candidates.size());
      if (amount != null) {
        candidates =
            candidates.stream()
                .filter(
                    order -> order.getAmount() != null && order.getAmount().compareTo(amount) == 0)
                .toList();
      }
      if (candidates.size() > 1) {
        candidates = resolvePreferredCandidate(chain, destinationAddress, candidates);
      }
      if (candidates.size() != 1) {
        log.warn(
            "链上支付匹配歧义，已跳过。chain={}, destinationAddress={}, tokenAddress={}, txHash={}, candidateCount={}",
            chain,
            destinationAddress,
            tokenAddress,
            txHash,
            candidates.size());
        return Optional.empty();
      }
    }

    PaymentOrder order = candidates.get(0);
    String orderLockKey = RedisKeyNamespace.creditOrderLock(properties, order.getCryptoOrderNo());
    return lockUtil.withLock(
        orderLockKey,
        2000,
        30,
        () -> recordChainPaymentLocked(
            order.getCryptoOrderNo(),
            chain,
            destinationAddress,
            txHash,
            amount,
            tokenAddress,
            sourceAddress,
            blockNumber));
  }

  private Optional<PaymentOrder> recordChainPaymentLocked(
      String cryptoOrderNo,
      String chain,
      String destinationAddress,
      String txHash,
      BigDecimal amount,
      String tokenAddress,
      String sourceAddress,
      Long blockNumber) {
    LocalDateTime now = LocalDateTime.now();
    PaymentOrder order =
        orderRepository
            .findByCryptoOrderNo(cryptoOrderNo)
            .orElseThrow(() -> new IllegalArgumentException("Crypto order not found: " + cryptoOrderNo));
    if (StringUtils.hasText(order.getPaymentTxHash())
        && !order.getPaymentTxHash().equalsIgnoreCase(txHash)) {
      log.info(
          "订单已存在不同入账交易，跳过新的链上支付事件。cryptoOrderNo={}, currentTxHash={}, incomingTxHash={}, status={}",
          order.getCryptoOrderNo(),
          order.getPaymentTxHash(),
          txHash,
          order.getStatus());
      return Optional.empty();
    }
    if (order.getStatus() == OrderStatus.PAID
        || order.getStatus() == OrderStatus.UNDERPAID
        || order.getStatus() == OrderStatus.OVERPAID
        || order.getStatus() == OrderStatus.CANCELLED) {
      log.info(
          "订单已存在入账结果，跳过新的链上支付事件。cryptoOrderNo={}, currentTxHash={}, incomingTxHash={}, status={}",
          order.getCryptoOrderNo(),
          order.getPaymentTxHash(),
          txHash,
          order.getStatus());
      return Optional.empty();
    }
    if (!replayProtectionService.claimTx(
        StringUtils.hasText(chain) ? chain : "UNKNOWN_CHAIN",
        txHash,
        chainReplayTtlHours(chain))) {
      log.info(
          "重复交易已跳过。chain={}, txHash={}, destinationAddress={}",
          chain,
          txHash,
          destinationAddress);
      return Optional.empty();
    }

    boolean latePayment = order.getExpireTime() != null && now.isAfter(order.getExpireTime());
    order.setPaymentTxHash(txHash);
    order.setRealAmount(amount);
    order.setPaidAt(now);
    order.setLatePayment(latePayment);
    order.setLatePaymentAt(latePayment ? now : null);
    applySettlementFeeResult(order, amount);
    if (latePayment) {
      // 延迟到账只做事实记录，订单状态和商户回调交给管理端人工确认后再推进。
      if (!orderRepository.savePaymentResultIfClaimable(order, paymentClaimableStatuses(), txHash)) {
        log.info(
            "延迟入账数据库条件更新未命中，跳过本次链上事件。cryptoOrderNo={}, txHash={}, status={}",
            order.getCryptoOrderNo(),
            txHash,
            order.getStatus());
        return Optional.empty();
      }
      auditService.record(
          "LATE_CHAIN_PAYMENT_RECORDED",
          "CRYPTO_ORDER",
          order.getCryptoOrderNo(),
          order.getStatus().name(),
          order);
      log.warn(
          "Recorded late chain payment without status transition. cryptoOrderNo={}, txHash={}, expectedAmount={}, realAmount={}, destinationAddress={}, currentStatus={}",
          order.getCryptoOrderNo(),
          txHash,
          order.getAmount(),
          amount,
          destinationAddress,
          order.getStatus());
      exceptionOrderService.createLatePaymentException(
          order,
          chain,
          sourceAddress,
          destinationAddress,
          txHash,
          amount,
          tokenAddress,
          blockNumber);
      activeDestinationAddressCache.unregister(order);
      return Optional.of(order);
    }

    OrderStatus paymentStatus = resolvePaymentAmountStatus(order, amount);
    if (order.getPaymentMethod() == PaymentMethod.CONTRACT && paymentStatus == OrderStatus.PAID) {
      // 智能合约路线先进入隔离确认态，后续由合约放行动作再推进到最终成功。
      order.setStatus(stateMachine.confirming(order.getStatus()));
    } else if (paymentStatus == OrderStatus.UNDERPAID) {
      order.setStatus(stateMachine.underpaid(order.getStatus()));
    } else if (paymentStatus == OrderStatus.OVERPAID) {
      order.setStatus(stateMachine.overpaid(order.getStatus()));
    } else {
      order.setStatus(stateMachine.paid(order.getStatus()));
    }
    if (!orderRepository.savePaymentResultIfClaimable(order, paymentClaimableStatuses(), txHash)) {
      log.info(
          "链上入账数据库条件更新未命中，跳过本次链上事件。cryptoOrderNo={}, txHash={}, status={}",
          order.getCryptoOrderNo(),
          txHash,
          order.getStatus());
      return Optional.empty();
    }
    auditService.record(
        "CHAIN_PAYMENT_RECORDED",
        "CRYPTO_ORDER",
        order.getCryptoOrderNo(),
        order.getStatus().name(),
        order);
    log.info(
        "Recorded chain payment. cryptoOrderNo={}, txHash={}, expectedAmount={}, realAmount={}, latePayment={}, destinationAddress={}, status={}",
        order.getCryptoOrderNo(),
        txHash,
        order.getAmount(),
        amount,
        latePayment,
        destinationAddress,
        order.getStatus());
    if (order.getPaymentMethod() != PaymentMethod.CONTRACT || order.getStatus() != OrderStatus.CONFIRMING) {
      activeDestinationAddressCache.unregister(order);
    }
    return Optional.of(order);
  }

  /** 将已监听到入账但命中 KYT 风险的订单挂起，等待运营人工处理。 */
  @Transactional
  public PaymentOrder markKytReview(String cryptoOrderNo, String reason) {
    PaymentOrder order =
        orderRepository
            .findByCryptoOrderNo(cryptoOrderNo)
            .orElseThrow(() -> new IllegalArgumentException("Crypto order not found: " + cryptoOrderNo));
    if (order.getStatus() == OrderStatus.EXPIRED || order.getStatus() == OrderStatus.CANCELLED) {
      log.info("Skip KYT review status transition for immutable order. cryptoOrderNo={}, status={}",
          cryptoOrderNo, order.getStatus());
      return order;
    }
    if (order.getStatus() != OrderStatus.KYT_REVIEW) {
      order.setStatus(stateMachine.kytReview(order.getStatus()));
      saveOrderWithStatusGuard(order, kytReviewStatuses(), "ORDER_KYT_REVIEW");
      auditService.record(
          "ORDER_KYT_REVIEW",
          "CRYPTO_ORDER",
          order.getCryptoOrderNo(),
          order.getStatus().name(),
          Map.of(
              "cryptoOrderNo", order.getCryptoOrderNo(),
              "merchantId", order.getMerchantId(),
              "merchantOrderNo", order.getMerchantOrderNo(),
              "paymentTxHash", order.getPaymentTxHash(),
              "reason", reason == null ? "" : reason));
      log.warn("Order moved to KYT_REVIEW. cryptoOrderNo={}, txHash={}, reason={}",
          order.getCryptoOrderNo(), order.getPaymentTxHash(), reason);
    }
    return order;
  }

  /**
   * 合约隔离资金完成放行后，将订单从 CONFIRMING/KYT_REVIEW 推进到最终支付状态。
   *
   * <p>这个方法只负责订单状态与审计，不直接做商户余额入账和回调；
   * 外层结算完成服务会在本方法成功返回后继续处理资金流水和通知。
   */
  @Transactional
  public Optional<PaymentOrder> finalizeContractPaymentRelease(String cryptoOrderNo) {
    PaymentOrder order =
        orderRepository
            .findByCryptoOrderNo(cryptoOrderNo)
            .orElseThrow(() -> new IllegalArgumentException("Crypto order not found: " + cryptoOrderNo));
    if (order.getPaymentMethod() != PaymentMethod.CONTRACT) {
      throw new IllegalStateException("Only contract payment order can be released: " + cryptoOrderNo);
    }
    if (order.isLatePayment() || order.getStatus() == OrderStatus.EXPIRED) {
      throw new IllegalStateException("Late or expired contract payment must be handled manually: " + cryptoOrderNo);
    }
    if (order.getStatus() == OrderStatus.PAID
        || order.getStatus() == OrderStatus.UNDERPAID
        || order.getStatus() == OrderStatus.OVERPAID) {
      log.info("合约订单已经是最终支付状态，跳过重复放行。cryptoOrderNo={}, status={}",
          order.getCryptoOrderNo(), order.getStatus());
      return Optional.empty();
    }
    if (order.getStatus() != OrderStatus.CONFIRMING && order.getStatus() != OrderStatus.KYT_REVIEW) {
      throw new IllegalStateException(
          "Contract payment order is not ready to release: "
              + cryptoOrderNo
              + ", status="
              + order.getStatus());
    }

    OrderStatus paymentStatus = resolvePaymentAmountStatus(order, order.getRealAmount());
    if (paymentStatus == OrderStatus.UNDERPAID) {
      order.setStatus(stateMachine.underpaid(order.getStatus()));
    } else if (paymentStatus == OrderStatus.OVERPAID) {
      order.setStatus(stateMachine.overpaid(order.getStatus()));
    } else {
      order.setStatus(stateMachine.paid(order.getStatus()));
    }
    saveOrderWithStatusGuard(order, contractReleaseStatuses(), "CONTRACT_PAYMENT_RELEASED");
    auditService.record(
        "CONTRACT_PAYMENT_RELEASED",
        "CRYPTO_ORDER",
        order.getCryptoOrderNo(),
        order.getStatus().name(),
        Map.of(
            "cryptoOrderNo", order.getCryptoOrderNo(),
            "merchantId", order.getMerchantId(),
            "merchantOrderNo", order.getMerchantOrderNo(),
            "paymentTxHash", order.getPaymentTxHash(),
            "realAmount", order.getRealAmount() == null ? "" : order.getRealAmount(),
            "status", order.getStatus().name()));
    log.info("合约订单放行后已推进到最终支付状态。cryptoOrderNo={}, status={}, txHash={}, realAmount={}",
        order.getCryptoOrderNo(), order.getStatus(), order.getPaymentTxHash(), order.getRealAmount());
    activeDestinationAddressCache.unregister(order);
    return Optional.of(order);
  }

  /** 根据平台支付单号查询订单。 */
  public PaymentOrder getOrder(String cryptoOrderNo) {
    PaymentOrder order =
        orderRepository
            .findByCryptoOrderNo(cryptoOrderNo)
        .orElseThrow(
            () -> new IllegalArgumentException("Crypto order not found: " + cryptoOrderNo));
    assertMerchantOwnership(order);
    return order;
  }

  /** 根据商户号和商户订单号查询订单。 */
  public PaymentOrder getOrder(String merchantId, String merchantOrderNo) {
    return orderRepository
        .findByMerchantIdAndMerchantOrderNo(merchantId, merchantOrderNo)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "Crypto order not found: merchantId=" + merchantId + ", merchantOrderNo=" + merchantOrderNo));
  }

  /** 根据链上交易哈希主动查询订单。 */
  public PaymentOrder getOrderByPaymentTxHash(String paymentTxHash) {
    PaymentOrder order =
        orderRepository
            .findByPaymentTxHash(paymentTxHash)
        .orElseThrow(
            () -> new BizException(
                ApiResponseCode.NOT_FOUND,
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Crypto order not found by txHash: " + paymentTxHash));
    assertMerchantOwnership(order);
    return order;
  }

  /** 为订单生成收银台 token。 */
  public String buildCashierToken(PaymentOrder order) {
    return cashierTokenService.createToken(
        order,
        Map.of(
            "cashierBaseUrl", properties.getCashierBaseUrl(),
            "tokenPrefix", properties.getCashier().getTokenPrefix()));
  }

  /** 组装收银台访问地址。 */
  public String buildCashierUrl(String cashierToken) {
    return properties.getCashierBaseUrl() + "/cashier/" + cashierToken;
  }

  private String generateCryptoOrderNo(LocalDateTime now) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    return "CP" + ORDER_TIME_FORMAT.format(now) + suffix;
  }

  private List<PaymentOrder> resolvePreferredCandidate(
      String chain, String destinationAddress, List<PaymentOrder> candidates) {
    List<PaymentOrder> leasedMatches =
        candidates.stream()
            .filter(order -> order.getPaymentMethod() == PaymentMethod.DERIVED_ADDRESS)
            .filter(order -> StringUtils.hasText(order.getDerivedAddressPoolKey()))
            .filter(
                order ->
                    derivedAddressInventoryService
                        .findByPoolKeyAndAddress(order.getDerivedAddressPoolKey(), destinationAddress)
                        .map(
                            record ->
                                StringUtils.hasText(record.getLeaseOrderNo())
                                    && order
                                        .getCryptoOrderNo()
                                        .equalsIgnoreCase(record.getLeaseOrderNo()))
                        .orElse(false))
            .toList();

    if (leasedMatches.size() == 1) {
      PaymentOrder chosen = leasedMatches.get(0);
      log.info(
          "链上支付多候选已按当前租约收敛。chain={}, destinationAddress={}, cryptoOrderNo={}, merchantOrderNo={}, candidateCount={}",
          chain,
          destinationAddress,
          chosen.getCryptoOrderNo(),
          chosen.getMerchantOrderNo(),
          candidates.size());
      return leasedMatches;
    }

    if (leasedMatches.size() > 1) {
      log.warn(
          "链上支付候选中存在多个当前租约匹配，保持歧义。chain={}, destinationAddress={}, leasedMatchCount={}, candidateCount={}",
          chain,
          destinationAddress,
          leasedMatches.size(),
          candidates.size());
    }

    return candidates;
  }

  private void saveOrderWithStatusGuard(
      PaymentOrder order, Set<OrderStatus> allowedStatuses, String eventType) {
    if (orderRepository.saveIfStatusIn(order, allowedStatuses)) {
      return;
    }
    PaymentOrder latest =
        orderRepository
            .findByCryptoOrderNo(order.getCryptoOrderNo())
            .orElse(null);
    String latestStatus = latest == null || latest.getStatus() == null ? "UNKNOWN" : latest.getStatus().name();
    log.warn(
        "订单状态数据库条件更新未命中。eventType={}, cryptoOrderNo={}, targetStatus={}, latestStatus={}",
        eventType,
        order.getCryptoOrderNo(),
        order.getStatus(),
        latestStatus);
    throw new IllegalStateException(
        "Order status changed concurrently, eventType=" + eventType + ", latestStatus=" + latestStatus);
  }

  private Set<OrderStatus> selectableStatuses() {
    return EnumSet.of(OrderStatus.CREATED, OrderStatus.METHOD_SELECTED, OrderStatus.WAITING_PAYMENT);
  }

  private Set<OrderStatus> detectedStatuses() {
    return EnumSet.of(OrderStatus.METHOD_SELECTED, OrderStatus.WAITING_PAYMENT, OrderStatus.DETECTED);
  }

  private Set<OrderStatus> paymentClaimableStatuses() {
    return EnumSet.of(
        OrderStatus.CREATED,
        OrderStatus.METHOD_SELECTED,
        OrderStatus.WAITING_PAYMENT,
        OrderStatus.DETECTED,
        OrderStatus.CONFIRMING,
        OrderStatus.KYT_REVIEW,
        OrderStatus.EXPIRED);
  }

  private Set<OrderStatus> kytReviewStatuses() {
    return EnumSet.of(
        OrderStatus.CREATED,
        OrderStatus.METHOD_SELECTED,
        OrderStatus.WAITING_PAYMENT,
        OrderStatus.DETECTED,
        OrderStatus.CONFIRMING,
        OrderStatus.PAID,
        OrderStatus.UNDERPAID,
        OrderStatus.OVERPAID);
  }

  private Set<OrderStatus> contractReleaseStatuses() {
    return EnumSet.of(OrderStatus.CONFIRMING, OrderStatus.KYT_REVIEW);
  }

  private OrderStatus resolvePaymentAmountStatus(PaymentOrder order, BigDecimal realAmount) {
    if (order.getAmount() == null || realAmount == null) {
      return OrderStatus.PAID;
    }
    int compared = realAmount.compareTo(order.getAmount());
    if (compared < 0) {
      return OrderStatus.UNDERPAID;
    }
    if (compared > 0) {
      return OrderStatus.OVERPAID;
    }
    return OrderStatus.PAID;
  }

  private void applyFeeSnapshot(PaymentOrder order, MerchantSettlementFeeSnapshot snapshot) {
    MerchantSettlementFeeSnapshot safeSnapshot =
        snapshot == null ? MerchantSettlementFeeSnapshot.zero() : snapshot;
    order.setTransactionFeeRate(safeSnapshot.transactionFeeRate());
    order.setMinimumFee(safeSnapshot.minimumFee());
    order.setFixedFee(safeSnapshot.fixedFee());
    order.setGatewayFee(safeSnapshot.gatewayFee());
    order.setTaxRate(safeSnapshot.taxRate());
    order.setFeeSettlementMode(
        StringUtils.hasText(safeSnapshot.feeSettlementMode())
            ? safeSnapshot.feeSettlementMode().trim().toUpperCase()
            : "PER_ORDER");
  }

  private void applySettlementFeeResult(PaymentOrder order, BigDecimal realAmount) {
    BigDecimal amountForFee = realAmount == null ? order.getAmount() : realAmount;
    MerchantSettlementFeeResult result =
        settlementFeeCalculator.calculate(
            amountForFee,
            new MerchantSettlementFeeSnapshot(
                order.getTransactionFeeRate(),
                order.getMinimumFee(),
                order.getFixedFee(),
                order.getGatewayFee(),
                order.getTaxRate(),
                order.getFeeSettlementMode()));
    order.setTransactionFee(result.transactionFee());
    order.setTaxFee(result.taxFee());
    order.setTotalFee(result.totalFee());
    order.setSettlementAmount(result.settlementAmount());
  }

  private void assertMerchantOwnership(PaymentOrder order) {
    String requesterMerchantId = request.getHeader(properties.getSignature().getMerchantIdHeader());
    if (!StringUtils.hasText(requesterMerchantId) || !StringUtils.hasText(order.getMerchantId())) {
      return;
    }
    if (!requesterMerchantId.trim().equalsIgnoreCase(order.getMerchantId().trim())) {
      throw new BizException(
          ApiResponseCode.UNAUTHORIZED,
          org.springframework.http.HttpStatus.FORBIDDEN,
          "access denied for merchant order");
    }
  }

  private long chainReplayTtlHours(String chain) {
    return Math.max(72L, properties.scannerForChain(chain).getChainReplayTtlHours());
  }
}
