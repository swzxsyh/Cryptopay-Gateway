package io.swzxsyh.payment.application;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.SolanaChainClient;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.config.CryptoPaymentProperties.ChainProfile;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import io.swzxsyh.payment.pending.PendingChainTransactionService;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import io.swzxsyh.payment.scanner.RedisScannerCheckpointService;
import io.swzxsyh.payment.solana.SolanaIncomingTransfer;
import io.swzxsyh.payment.solana.SolanaTransferParseService;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Solana 监听器，独立于 EVM 扫描器处理 SPL / SOL 入账。 */
@Slf4j
@Service
public class SolanaWatcherService implements ChainWatcher {

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final PaymentOrderRepository orderRepository;
  private final RedisScannerCheckpointService redisCheckpointService;
  private final LockUtil lockUtil;
  private final SolanaTransferParseService transferParseService;
  private final ChainPaymentEventPublisher chainPaymentEventPublisher;
  private final PendingChainTransactionService pendingTransactionService;
  private final ConcurrentMap<String, Instant> lastChainScanTimes = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, Instant> chainCooldownUntil = new ConcurrentHashMap<>();

  public SolanaWatcherService(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      PaymentOrderRepository orderRepository,
      RedisScannerCheckpointService redisCheckpointService,
      LockUtil lockUtil,
      SolanaTransferParseService transferParseService,
      ChainPaymentEventPublisher chainPaymentEventPublisher,
      PendingChainTransactionService pendingTransactionService) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.orderRepository = orderRepository;
    this.redisCheckpointService = redisCheckpointService;
    this.lockUtil = lockUtil;
    this.transferParseService = transferParseService;
    this.chainPaymentEventPublisher = chainPaymentEventPublisher;
    this.pendingTransactionService = pendingTransactionService;
  }

  @Override
  public boolean supports(ChainProfile profile) {
    return profile != null
        && profile.isEnabled()
        && StringUtils.hasText(profile.getChain())
        && chainClientFactory.get(profile.getChain()).family() == ChainFamily.SOLANA;
  }

  @Override
  public void bootstrap(ChainProfile profile) {
    redisCheckpointService.bootstrap(profile.getChain());
    redisCheckpointService.flushIfNeeded(profile.getChain(), true);
    log.info("Solana 监听器已准备。chain={}, rpcUrl={}", profile.getChain(), profile.getRpcUrl());
  }

  @Override
  public void scan(ChainProfile profile) {
    if (!shouldScanChainNow(profile.getChain()) || isChainCoolingDown(profile.getChain())) {
      return;
    }
    String lockKey = RedisKeyNamespace.solanaScannerLock(properties, profile.getChain());
    try {
      lockUtil.withLock(lockKey, 2000, 10, () -> {
        scanChain(profile);
        return null;
      });
    } catch (IllegalStateException ex) {
      log.debug("Solana 扫描锁被占用，本轮跳过。chain={}, lockKey={}", profile.getChain(), lockKey);
    }
  }

  private void scanChain(ChainProfile profile) {
    try {
      var client = chainClientFactory.get(profile.getChain());
      if (client.family() != ChainFamily.SOLANA || !(client instanceof SolanaChainClient solanaClient)) {
        return;
      }

      long latestSlot = solanaClient.getLatestBlockNumber().longValue();
      redisCheckpointService.updateObservedBlock(profile.getChain(), latestSlot);

      int confirmationDepth = resolveConfirmationDepth(profile);
      pendingTransactionService.advanceReadyTransactions(profile.getChain(), latestSlot);
      long confirmedSlot = latestSlot - confirmationDepth;
      long lastConfirmedSlot = redisCheckpointService.getLastConfirmedBlock(profile.getChain());

      log.info(
          "Solana 扫描范围计算完成。chain={}, latestSlot={}, confirmationDepth={}, confirmedSlot={}, lastConfirmedSlot={}",
          profile.getChain(), latestSlot, confirmationDepth, confirmedSlot, lastConfirmedSlot);

      if (confirmedSlot < 1L) {
        return;
      }

      if (lastConfirmedSlot <= 0L) {
        redisCheckpointService.updateConfirmedBlock(profile.getChain(), confirmedSlot);
        redisCheckpointService.flushIfNeeded(profile.getChain(), true);
        log.info("Solana 首次初始化 checkpoint。chain={}, confirmedSlot={}", profile.getChain(), confirmedSlot);
        return;
      }

      List<PaymentOrder> orders = openSolanaOrders(profile.getChain());
      if (orders.isEmpty()) {
        redisCheckpointService.updateConfirmedBlock(profile.getChain(), confirmedSlot);
        redisCheckpointService.flushIfNeeded(profile.getChain(), false);
        return;
      }

      Set<String> destinations = candidateDestinations(orders);
      long batchLimit = Math.max(20L, properties.scannerForChain(profile.getChain()).getLogScanBatchBlocks());
      long processedMaxSlot = lastConfirmedSlot;
      for (String destination : destinations) {
        processedMaxSlot =
            Math.max(
                processedMaxSlot,
                scanDestination(
                    solanaClient,
                    profile.getChain(),
                    destination,
                    confirmedSlot,
                    lastConfirmedSlot,
                    (int) batchLimit));
      }

      if (processedMaxSlot > lastConfirmedSlot) {
        redisCheckpointService.updateConfirmedBlock(profile.getChain(), processedMaxSlot);
      }
      redisCheckpointService.flushIfNeeded(profile.getChain(), false);
      scanUnconfirmedSlots(solanaClient, profile.getChain(), destinations, confirmedSlot, latestSlot, confirmationDepth, (int) batchLimit);
      clearChainFailure(profile.getChain());
    } catch (Exception ex) {
      markChainFailure(profile.getChain(), ex);
    }
  }

  private void scanUnconfirmedSlots(
      SolanaChainClient client,
      String chain,
      Set<String> destinations,
      long confirmedSlot,
      long latestSlot,
      int confirmationDepth,
      int pageLimit) {
    long fromExclusive = Math.max(0L, confirmedSlot);
    for (String destination : destinations) {
      scanDestinationPending(client, chain, destination, fromExclusive, latestSlot, confirmationDepth, pageLimit);
    }
  }

  private boolean isChainCoolingDown(String chain) {
    Instant until = chainCooldownUntil.get(chain.toUpperCase(Locale.ROOT));
    if (until == null) {
      return false;
    }
    if (until.isAfter(Instant.now())) {
      log.debug("Solana 扫描处于失败冷却期，本轮跳过。chain={}, retryAfter={}", chain, until);
      return true;
    }
    chainCooldownUntil.remove(chain.toUpperCase(Locale.ROOT));
    return false;
  }

  private void markChainFailure(String chain, Exception ex) {
    long cooldownSeconds = Math.max(1L, properties.scannerForChain(chain).getFailureCooldownSeconds());
    Instant retryAfter = Instant.now().plusSeconds(cooldownSeconds);
    chainCooldownUntil.put(chain.toUpperCase(Locale.ROOT), retryAfter);
    if (isConfigurationOrProviderAccessError(ex)) {
      log.warn(
          "Solana 扫描失败，疑似 RPC/节点配置或供应商权限问题，进入冷却期。chain={}, retryAfter={}, error={}",
          chain,
          retryAfter,
          ex.getMessage());
      log.debug("Solana 扫描配置类异常堆栈。chain={}", chain, ex);
      return;
    }
    log.error("Solana 区块扫描失败，进入冷却期。chain={}, retryAfter={}, error={}",
        chain, retryAfter, ex.getMessage(), ex);
  }

  private void clearChainFailure(String chain) {
    chainCooldownUntil.remove(chain.toUpperCase(Locale.ROOT));
  }

  private boolean isConfigurationOrProviderAccessError(Exception ex) {
    String message = ex.getMessage();
    return message != null
        && (message.contains("not enabled for this app")
            || message.contains("HTTP connect timed out")
            || message.contains("Failed to call Solana RPC method"));
  }

  private long scanDestination(
      SolanaChainClient client,
      String chain,
      String destination,
      long confirmedSlot,
      long lastConfirmedSlot,
      int pageLimit) {
    String before = null;
    long maxProcessedSlot = lastConfirmedSlot;
    for (int page = 1; page <= 20; page++) {
      var signatures = client.getSignaturesForAddress(destination, pageLimit, before);
      if (signatures == null || signatures.isEmpty()) {
        break;
      }

      String lastSignature = null;
      boolean reachedHistoryBoundary = false;
      for (JsonNode item : signatures) {
        lastSignature = item.path("signature").asText("");
        long slot = item.path("slot").asLong(0L);
        if (!StringUtils.hasText(lastSignature)) {
          continue;
        }
        if (slot <= lastConfirmedSlot) {
          reachedHistoryBoundary = true;
          continue;
        }
        if (slot > confirmedSlot) {
          continue;
        }
        JsonNode transaction = client.getTransaction(lastSignature);
        java.util.Optional<SolanaIncomingTransfer> parsed =
            transferParseService.parse(transaction, destination, null);
        if (parsed.isEmpty()) {
          continue;
        }
        SolanaIncomingTransfer transfer = parsed.get();
        ChainPaymentEvent event = toSolanaEvent(chain, transfer);
        chainPaymentEventPublisher.publish(event);
        log.info(
            "Solana 入账事件已解析待发布。chain={}, destination={}, tokenAddress={}, txHash={}, amount={}, nativeTransfer={}",
            chain,
            transfer.destinationAddress(),
            transfer.tokenAddress(),
            transfer.signature(),
            transfer.amount(),
            transfer.nativeTransfer());
        maxProcessedSlot = Math.max(maxProcessedSlot, slot);
      }

      if (!StringUtils.hasText(lastSignature) || reachedHistoryBoundary || signatures.size() < pageLimit) {
        break;
      }
      before = lastSignature;
    }
    return maxProcessedSlot;
  }

  private void scanDestinationPending(
      SolanaChainClient client,
      String chain,
      String destination,
      long fromExclusive,
      long latestSlot,
      int confirmationDepth,
      int pageLimit) {
    String before = null;
    for (int page = 1; page <= 5; page++) {
      var signatures = client.getSignaturesForAddress(destination, pageLimit, before);
      if (signatures == null || signatures.isEmpty()) {
        break;
      }
      String lastSignature = null;
      boolean reachedBoundary = false;
      for (JsonNode item : signatures) {
        lastSignature = item.path("signature").asText("");
        long slot = item.path("slot").asLong(0L);
        if (!StringUtils.hasText(lastSignature)) {
          continue;
        }
        if (slot <= fromExclusive) {
          reachedBoundary = true;
          continue;
        }
        if (slot > latestSlot) {
          continue;
        }
        JsonNode transaction = client.getTransaction(lastSignature);
        java.util.Optional<SolanaIncomingTransfer> parsed =
            transferParseService.parse(transaction, destination, null);
        if (parsed.isEmpty()) {
          continue;
        }
        pendingTransactionService.observe(toSolanaEvent(chain, parsed.get()), confirmationDepth, latestSlot);
      }
      if (!StringUtils.hasText(lastSignature) || reachedBoundary || signatures.size() < pageLimit) {
        break;
      }
      before = lastSignature;
    }
  }

  private ChainPaymentEvent toSolanaEvent(String chain, SolanaIncomingTransfer transfer) {
    return ChainPaymentEvent.of(
        transfer.nativeTransfer() ? "SOLANA_NATIVE_SCAN" : "SOLANA_SPL_SCAN",
        chain,
        null,
        StringUtils.hasText(transfer.tokenAddress()) ? transfer.tokenAddress() : null,
        transfer.signature(),
        transfer.sourceAddress(),
        transfer.destinationAddress(),
        transfer.amount(),
        transfer.slot(),
        null,
        transfer.blockTimestamp());
  }

  private Set<String> candidateDestinations(List<PaymentOrder> orders) {
    Set<String> destinations = new LinkedHashSet<>();
    for (PaymentOrder order : orders) {
      if (StringUtils.hasText(order.getPaymentAddress())) {
        destinations.add(order.getPaymentAddress());
      }
      if (StringUtils.hasText(order.getContractAddress())) {
        destinations.add(order.getContractAddress());
      }
    }
    return destinations;
  }

  private List<PaymentOrder> openSolanaOrders(String chain) {
    return orderRepository.findActiveByChain(chain).stream()
        .filter(Objects::nonNull)
        .filter(order -> StringUtils.hasText(order.getPaymentAddress()) || StringUtils.hasText(order.getContractAddress()))
        .toList();
  }

  private int resolveConfirmationDepth(ChainProfile profile) {
    int depth = Math.max(0, properties.scannerForChain(profile.getChain()).getConfirmationDepth());
    if (profile.getConfirmationDepth() != null) {
      depth = Math.max(depth, Math.max(0, profile.getConfirmationDepth()));
    }
    return depth;
  }

  private boolean shouldScanChainNow(String chain) {
    var scanner = properties.scannerForChain(chain);
    long intervalSeconds = Math.max(1L, scanner.getScanIntervalSeconds());
    Instant now = Instant.now();
    Instant previous = lastChainScanTimes.get(chain.toUpperCase(Locale.ROOT));
    if (previous != null && previous.plusSeconds(intervalSeconds).isAfter(now)) {
      return false;
    }
    lastChainScanTimes.put(chain.toUpperCase(Locale.ROOT), now);
    return true;
  }

}
