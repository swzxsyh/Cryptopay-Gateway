package io.swzxsyh.payment.pending;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.PendingChainTransactionMapper;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import io.swzxsyh.payment.state.PaymentOrderStateMachine;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 未确认链上交易服务。
 *
 * <p>这个服务只做“提前发现、确认数展示、达标后投递到现有入账链路”，不直接把订单改成 PAID。
 */
@Slf4j
@Service
public class PendingChainTransactionService {

  private final PendingChainTransactionMapper mapper;
  private final PaymentOrderRepository orderRepository;
  private final PaymentOrderStateMachine stateMachine;
  private final PaymentAuditService auditService;
  private final ObjectProvider<ChainPaymentEventPublisher> eventPublisherProvider;
  private final PendingChainTransactionVerifier transactionVerifier;
  private final RedisUtil redisUtil;
  private final CryptoPaymentProperties properties;

  public PendingChainTransactionService(
      PendingChainTransactionMapper mapper,
      PaymentOrderRepository orderRepository,
      PaymentOrderStateMachine stateMachine,
      PaymentAuditService auditService,
      ObjectProvider<ChainPaymentEventPublisher> eventPublisherProvider,
      PendingChainTransactionVerifier transactionVerifier,
      RedisUtil redisUtil,
      CryptoPaymentProperties properties) {
    this.mapper = mapper;
    this.orderRepository = orderRepository;
    this.stateMachine = stateMachine;
    this.auditService = auditService;
    this.eventPublisherProvider = eventPublisherProvider;
    this.transactionVerifier = transactionVerifier;
    this.redisUtil = redisUtil;
    this.properties = properties;
  }

  /** 记录未确认交易，并在能唯一匹配订单时把订单推进到 DETECTED。 */
  @Transactional
  public Optional<PendingChainTransaction> observe(ChainPaymentEvent event, int targetConfirmations, long latestBlock) {
    if (event == null || !StringUtils.hasText(event.chain()) || !StringUtils.hasText(event.txHash())) {
      return Optional.empty();
    }
    PendingChainTransaction pending = find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex()))
        .orElseGet(() -> insertPending(event, targetConfirmations));
    refreshConfirmations(pending, latestBlock);
    detectOrderWhenSafe(pending);
    return Optional.of(pending);
  }

  /** 按最新块高度推进确认数，达到安全确认后投递给原有入账链路。 */
  @Transactional
  public void advanceReadyTransactions(String chain, long latestBlock) {
    if (!StringUtils.hasText(chain) || latestBlock <= 0) {
      return;
    }
    List<PendingChainTransaction> pendingList =
        mapper.selectList(
            Wrappers.<PendingChainTransaction>lambdaQuery()
                .eq(PendingChainTransaction::getChain, chain)
                .eq(PendingChainTransaction::getStatus, PendingChainTransactionStatus.PENDING.name())
                .le(PendingChainTransaction::getBlockNumber, latestBlock)
                .last("LIMIT 200"));
    for (PendingChainTransaction pending : pendingList) {
      refreshConfirmations(pending, latestBlock);
      // 使用现有 confirmedHeight 口径：latestBlock - txBlock >= target，避免比旧扫描链路更早认账。
      long safeDistance = latestBlock - nullToZero(pending.getBlockNumber());
      int target = Math.max(1, pending.getTargetConfirmations() == null ? 1 : pending.getTargetConfirmations());
      if (safeDistance < target) {
        continue;
      }
      if (!transactionVerifier.isStillCanonical(pending, latestBlock)) {
        log.debug(
            "待确认交易达到确认数但链上复核未通过，暂不投递。chain={}, txHash={}, logIndex={}, blockNumber={}, latestBlock={}",
            pending.getChain(),
            pending.getTxHash(),
            pending.getLogIndex(),
            pending.getBlockNumber(),
            latestBlock);
        continue;
      }
      ChainPaymentEventPublisher eventPublisher = eventPublisherProvider.getIfAvailable();
      if (eventPublisher == null) {
        throw new IllegalStateException(
            "ChainPaymentEventPublisher is required before dispatching pending transaction. chain="
                + pending.getChain()
                + ", txHash="
                + pending.getTxHash());
      }
      pending.setStatus(PendingChainTransactionStatus.DISPATCHED.name());
      pending.setCurrentConfirmations(readRedisConfirmations(pending).orElse(pending.getCurrentConfirmations()));
      pending.setLastDispatchedAt(LocalDateTime.now());
      pending.setUpdatedAt(LocalDateTime.now());
      mapper.updateById(pending);
      eventPublisher.publish(toEvent(pending));
      log.info(
          "未确认交易已达到确认数，已投递到原有入账处理链路。chain={}, txHash={}, logIndex={}, blockNumber={}, latestBlock={}, targetConfirmations={}, currentConfirmations={}",
          pending.getChain(),
          pending.getTxHash(),
          pending.getLogIndex(),
          pending.getBlockNumber(),
          latestBlock,
          target,
          pending.getCurrentConfirmations());
    }
  }

  /** 最终入账链路匹配成功后，回写 pending 状态，便于收银台和运营侧观察。 */
  @Transactional
  public void markMatched(ChainPaymentEvent event, String cryptoOrderNo) {
    find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex()))
        .ifPresent(
            pending -> {
              pending.setStatus(PendingChainTransactionStatus.MATCHED.name());
              pending.setCurrentConfirmations(readRedisConfirmations(pending).orElse(pending.getCurrentConfirmations()));
              pending.setMatchedCryptoOrderNo(cryptoOrderNo);
              pending.setUpdatedAt(LocalDateTime.now());
              mapper.updateById(pending);
            });
  }

  /** 查询订单当前待确认交易。 */
  public Optional<PendingChainTransaction> findLatestByOrderNo(String cryptoOrderNo) {
    if (!StringUtils.hasText(cryptoOrderNo)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        mapper.selectOne(
            Wrappers.<PendingChainTransaction>lambdaQuery()
                .eq(PendingChainTransaction::getMatchedCryptoOrderNo, cryptoOrderNo)
                .orderByDesc(PendingChainTransaction::getCreatedAt)
                .last("LIMIT 1")))
        .map(this::enrichFromRedis);
  }

  private PendingChainTransaction insertPending(ChainPaymentEvent event, int targetConfirmations) {
    LocalDateTime now = LocalDateTime.now();
    PendingChainTransaction pending = new PendingChainTransaction();
    pending.setChain(event.chain());
    pending.setTxHash(event.txHash());
    pending.setLogIndex(normalizeLogIndex(event.logIndex()));
    pending.setSource(event.source());
    pending.setToken(event.token());
    pending.setTokenAddress(event.tokenAddress());
    pending.setFromAddress(event.sourceAddress());
    pending.setToAddress(event.destinationAddress());
    pending.setAmount(event.amount());
    pending.setBlockNumber(event.blockNumber());
    pending.setBlockTimestamp(
        event.blockTimestamp() == null
            ? null
            : LocalDateTime.ofInstant(event.blockTimestamp(), ZoneId.systemDefault()));
    pending.setTargetConfirmations(Math.max(1, targetConfirmations));
    pending.setCurrentConfirmations(0);
    pending.setStatus(PendingChainTransactionStatus.PENDING.name());
    pending.setObservedAt(now);
    pending.setCreatedAt(now);
    pending.setUpdatedAt(now);
    try {
      mapper.insert(pending);
      log.info(
          "提前发现链上交易，已记录待确认任务。chain={}, txHash={}, logIndex={}, to={}, amount={}, blockNumber={}, targetConfirmations={}",
          pending.getChain(),
          pending.getTxHash(),
          pending.getLogIndex(),
          pending.getToAddress(),
          pending.getAmount(),
          pending.getBlockNumber(),
          pending.getTargetConfirmations());
      return pending;
    } catch (DuplicateKeyException ex) {
      return find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex())).orElseThrow(() -> ex);
    }
  }

  private void refreshConfirmations(PendingChainTransaction pending, long latestBlock) {
    if (pending.getBlockNumber() == null || latestBlock <= 0) {
      return;
    }
    int confirmations = (int) Math.max(0, latestBlock - pending.getBlockNumber() + 1);
    pending.setCurrentConfirmations(confirmations);
    writeRedisConfirmations(pending, confirmations, latestBlock);
  }

  private void detectOrderWhenSafe(PendingChainTransaction pending) {
    if (StringUtils.hasText(pending.getMatchedCryptoOrderNo())) {
      return;
    }
    List<PaymentOrder> candidates =
        orderRepository
            .findByChainAndDestinationAddressAndTokenAddress(
                pending.getChain(), pending.getToAddress(), pending.getTokenAddress())
            .stream()
            .filter(order -> stateMachine.canEnterDetected(order.getStatus()))
            .filter(order -> !StringUtils.hasText(order.getPaymentTxHash())
                || order.getPaymentTxHash().equalsIgnoreCase(pending.getTxHash()))
            .filter(order -> amountMatches(order, pending.getAmount()))
            .toList();
    if (candidates.size() != 1) {
      log.debug(
          "待确认交易暂不推进订单 DETECTED，候选不唯一或为空。chain={}, txHash={}, to={}, amount={}, candidateCount={}",
          pending.getChain(),
          pending.getTxHash(),
          pending.getToAddress(),
          pending.getAmount(),
          candidates.size());
      return;
    }
    PaymentOrder order = candidates.get(0);
    if (!StringUtils.hasText(order.getPaymentTxHash())) {
      order.setPaymentTxHash(pending.getTxHash());
    }
    order.setStatus(stateMachine.detected(order.getStatus()));
    boolean updated =
        orderRepository.saveIfStatusIn(
            order,
            EnumSet.of(
                OrderStatus.METHOD_SELECTED,
                OrderStatus.WAITING_PAYMENT,
                OrderStatus.DETECTED));
    if (!updated) {
      log.info(
          "待确认交易推进 DETECTED 时数据库条件更新未命中，跳过。cryptoOrderNo={}, chain={}, txHash={}",
          order.getCryptoOrderNo(),
          pending.getChain(),
          pending.getTxHash());
      return;
    }
    pending.setMatchedCryptoOrderNo(order.getCryptoOrderNo());
    pending.setCurrentConfirmations(readRedisConfirmations(pending).orElse(pending.getCurrentConfirmations()));
    pending.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(pending);
    auditService.record(
        "PENDING_CHAIN_PAYMENT_DETECTED",
        "CRYPTO_ORDER",
        order.getCryptoOrderNo(),
        order.getStatus().name(),
        order);
    log.info(
        "链上交易提前匹配订单，订单进入 DETECTED。cryptoOrderNo={}, chain={}, txHash={}, currentConfirmations={}, targetConfirmations={}",
        order.getCryptoOrderNo(),
        pending.getChain(),
        pending.getTxHash(),
        pending.getCurrentConfirmations(),
        pending.getTargetConfirmations());
  }

  private boolean amountMatches(PaymentOrder order, BigDecimal amount) {
    return amount == null || order.getAmount() == null || order.getAmount().compareTo(amount) == 0;
  }

  private ChainPaymentEvent toEvent(PendingChainTransaction pending) {
    return ChainPaymentEvent.of(
        pending.getSource(),
        pending.getChain(),
        pending.getToken(),
        pending.getTokenAddress(),
        pending.getTxHash(),
        pending.getFromAddress(),
        pending.getToAddress(),
        pending.getAmount(),
        pending.getBlockNumber(),
        pending.getLogIndex(),
        pending.getBlockTimestamp() == null
            ? null
            : pending.getBlockTimestamp().atZone(ZoneId.systemDefault()).toInstant());
  }

  private Optional<PendingChainTransaction> find(String chain, String txHash, Long logIndex) {
    return Optional.ofNullable(
        mapper.selectOne(
            Wrappers.<PendingChainTransaction>lambdaQuery()
                .eq(PendingChainTransaction::getChain, chain)
                .eq(PendingChainTransaction::getTxHash, txHash)
                .eq(PendingChainTransaction::getLogIndex, logIndex)
                .last("LIMIT 1")));
  }

  private Long normalizeLogIndex(Long logIndex) {
    return logIndex == null || logIndex < 0 ? 0L : logIndex;
  }

  private long nullToZero(Long value) {
    return value == null ? 0L : value;
  }

  private PendingChainTransaction enrichFromRedis(PendingChainTransaction pending) {
    readRedisConfirmations(pending).ifPresent(pending::setCurrentConfirmations);
    return pending;
  }

  private void writeRedisConfirmations(PendingChainTransaction pending, int confirmations, long latestBlock) {
    String key = redisKey(pending);
    redisUtil.mapCache(key).put("currentConfirmations", String.valueOf(confirmations));
    redisUtil.mapCache(key).put("targetConfirmations", String.valueOf(pending.getTargetConfirmations()));
    redisUtil.mapCache(key).put("latestBlock", String.valueOf(latestBlock));
    redisUtil.mapCache(key).put("updatedAt", String.valueOf(LocalDateTime.now()));
  }

  private Optional<Integer> readRedisConfirmations(PendingChainTransaction pending) {
    Object value = redisUtil.mapCache(redisKey(pending)).get("currentConfirmations");
    if (value == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(Integer.parseInt(String.valueOf(value)));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private String redisKey(PendingChainTransaction pending) {
    return RedisKeyNamespace.pendingConfirmation(
        properties,
        pending.getChain(),
        pending.getTxHash(),
        normalizeLogIndex(pending.getLogIndex()));
  }
}
