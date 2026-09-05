package io.swzxsyh.payment.application;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.config.CryptoPaymentProperties.ChainProfile;
import io.swzxsyh.payment.config.CryptoPaymentProperties.Scanner;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import io.swzxsyh.payment.pending.PendingChainTransactionService;
import io.swzxsyh.payment.scanner.RedisScannerCheckpointService;
import io.swzxsyh.payment.scanner.Erc20TransferScanService;
import io.swzxsyh.payment.alert.PaymentAlertService;
import io.swzxsyh.payment.application.subscription.SubscriptionEventScanService;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.protocol.core.methods.response.EthBlock;

/** 多链区块监听与链上收款识别服务。 */
@Slf4j
@Service
public class CryptoWatcherService implements ChainWatcher {

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final PaymentAlertService alertService;
  private final RedisScannerCheckpointService redisCheckpointService;
  private final Erc20TransferScanService erc20TransferScanService;
  private final SubscriptionEventScanService subscriptionEventScanService;
  private final ChainPaymentEventPublisher chainPaymentEventPublisher;
  private final PendingChainTransactionService pendingTransactionService;
  private final LockUtil lockUtil;
  private final EvmWebSocketObserverCoordinator webSocketObserverCoordinator;
  private final Map<String, Instant> lastChainScanTimes = new ConcurrentHashMap<>();
  private final Map<String, Instant> chainCooldownUntil = new ConcurrentHashMap<>();

  public CryptoWatcherService(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      PaymentAlertService alertService,
      RedisScannerCheckpointService redisCheckpointService,
      Erc20TransferScanService erc20TransferScanService,
      SubscriptionEventScanService subscriptionEventScanService,
      ChainPaymentEventPublisher chainPaymentEventPublisher,
      PendingChainTransactionService pendingTransactionService,
      LockUtil lockUtil,
      EvmWebSocketObserverCoordinator webSocketObserverCoordinator) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.alertService = alertService;
    this.redisCheckpointService = redisCheckpointService;
    this.erc20TransferScanService = erc20TransferScanService;
    this.subscriptionEventScanService = subscriptionEventScanService;
    this.chainPaymentEventPublisher = chainPaymentEventPublisher;
    this.pendingTransactionService = pendingTransactionService;
    this.lockUtil = lockUtil;
    this.webSocketObserverCoordinator = webSocketObserverCoordinator;
  }

  @Override
  public boolean supports(ChainProfile profile) {
    return profile != null
        && profile.isEnabled()
        && StringUtils.hasText(profile.getChain())
        && chainClientFactory.get(profile.getChain()).isEvmFamily();
  }

  /** 应用启动时初始化单条 EVM 链监听器。 */
  @Override
  public void bootstrap(ChainProfile profile) {
    redisCheckpointService.bootstrap(profile.getChain());
    redisCheckpointService.flushIfNeeded(profile.getChain(), true);
    log.info(
        "准备 EVM 链监听。chain={}, rpcUrl={}, wsUrlPresent={}, confirmationDepth={}",
        profile.getChain(),
        profile.getRpcUrl(),
        StringUtils.hasText(profile.getWsUrl()),
        resolveConfirmationDepth(profile));
    Scanner scanner = properties.scannerForChain(profile.getChain());
    if (scanner.isWebsocketEnabled() && StringUtils.hasText(profile.getWsUrl())) {
      webSocketObserverCoordinator.maintain(profile);
    } else {
      log.info("EVM 链监听以仅 RPC 模式启动。chain={}", profile.getChain());
    }
  }

  /** 按确认数扫描单条 EVM 链已确认区块。 */
  @Override
  public void scan(ChainProfile profile) {
    String chain = profile.getChain();
    webSocketObserverCoordinator.maintain(profile);
    if (!shouldScanChainNow(chain) || isChainCoolingDown(chain)) {
      return;
    }
    int confirmationDepth = resolveConfirmationDepth(profile);
    String lockKey = RedisKeyNamespace.evmScannerLock(properties, chain);
    try {
      log.debug("准备扫描 EVM 链。chain={}, confirmationDepth={}, lockKey={}", chain, confirmationDepth, lockKey);
      lockUtil.withLock(lockKey, 2000, 10, () -> scanChain(profile, confirmationDepth));
    } catch (IllegalStateException ex) {
      log.debug("EVM 扫描锁被占用，本轮跳过。chain={}, lockKey={}", chain, lockKey);
    }
  }

  private boolean shouldScanChainNow(String chain) {
    Scanner scanner = properties.scannerForChain(chain);
    long intervalSeconds = Math.max(1L, scanner.getScanIntervalSeconds());
    Instant now = Instant.now();
    Instant previous = lastChainScanTimes.get(chain.toUpperCase());
    if (previous != null && previous.plusSeconds(intervalSeconds).isAfter(now)) {
      log.debug(
          "单链扫描间隔未到，本轮跳过。chain={}, intervalSeconds={}, lastScanAt={}",
          chain,
          intervalSeconds,
          previous);
      return false;
    }
    lastChainScanTimes.put(chain.toUpperCase(), now);
    return true;
  }

  private int resolveConfirmationDepth(ChainProfile profile) {
    Scanner scanner = properties.scannerForChain(profile.getChain());
    int depth = Math.max(0, scanner.getConfirmationDepth());
    if (profile.getConfirmationDepth() != null) {
      depth = Math.max(depth, Math.max(0, profile.getConfirmationDepth()));
    }
    int tokenDepth =
        properties.getTokenProfiles().stream()
            .filter(tokenProfile -> tokenProfile != null)
            .filter(tokenProfile -> profile.getChain().equalsIgnoreCase(tokenProfile.getChain()))
            .map(
                CryptoPaymentProperties.TokenProfile
                    ::getConfirmationDepth)
            .filter(java.util.Objects::nonNull)
            .mapToInt(value -> Math.max(0, value))
            .max()
            .orElse(0);
    return Math.max(depth, tokenDepth);
  }

  private void scanChain(ChainProfile profile, int confirmationDepth) {
    try {
      ChainClient chainClient = chainClientFactory.get(profile.getChain());
      BigInteger latest = chainClient.getLatestBlockNumber();
      if (latest == null) {
        log.warn("无法获取最新区块高度，跳过本轮扫描。chain={}", profile.getChain());
        return;
      }

      redisCheckpointService.updateObservedBlock(profile.getChain(), latest.longValue());
      pendingTransactionService.advanceReadyTransactions(profile.getChain(), latest.longValue());
      long confirmedHeight = latest.longValue() - confirmationDepth;
      long lastConfirmedBlock = redisCheckpointService.getLastConfirmedBlock(profile.getChain());
      log.info(
          "确认块扫描范围计算完成。chain={}, latestBlock={}, confirmationDepth={}, confirmedHeight={}, lastConfirmedBlock={}",
          profile.getChain(),
          latest,
          confirmationDepth,
          confirmedHeight,
          lastConfirmedBlock);
      if (confirmedHeight < 1L) {
        log.debug("确认高度不足，暂不扫描。chain={}, confirmedHeight={}", profile.getChain(), confirmedHeight);
        return;
      }
      if (lastConfirmedBlock <= 0L) {
        redisCheckpointService.updateConfirmedBlock(profile.getChain(), confirmedHeight);
        redisCheckpointService.flushIfNeeded(profile.getChain(), true);
        log.info(
            "首次初始化扫描 checkpoint。chain={}, latestBlock={}, confirmationDepth={}, initializedConfirmedBlock={}。后续订单将从该高度之后开始监听。",
            profile.getChain(),
            latest,
            confirmationDepth,
            confirmedHeight);
        return;
      }

      long nextBlock = lastConfirmedBlock + 1L;
      if (nextBlock < 1L) {
        nextBlock = 1L;
      }
      Scanner scanner = properties.scannerForChain(profile.getChain());
      long backfillBlocks = Math.max(0L, scanner.getBackfillBlocks());
      long scanStartBlock = Math.max(1L, nextBlock - backfillBlocks);
      if (nextBlock > confirmedHeight) {
        if (backfillBlocks <= 0L) {
          log.debug(
              "暂无新的确认块需要处理。chain={}, nextBlock={}, confirmedHeight={}",
              profile.getChain(),
              nextBlock,
              confirmedHeight);
          return;
        }
        scanStartBlock = Math.max(1L, confirmedHeight - backfillBlocks + 1L);
        log.debug(
            "暂无新确认块，执行最近区块回补扫描。chain={}, fromBlock={}, toBlock={}, backfillBlocks={}",
            profile.getChain(),
            scanStartBlock,
            confirmedHeight,
            backfillBlocks);
      }

      log.debug(
          "开始补扫确认块。chain={}, fromBlock={}, nextNewBlock={}, toBlock={}, backfillBlocks={}",
          profile.getChain(),
          scanStartBlock,
          nextBlock,
          confirmedHeight,
          backfillBlocks);

      long batchSize = Math.max(1L, scanner.getLogScanBatchBlocks());
      for (long batchStart = scanStartBlock; batchStart <= confirmedHeight; batchStart += batchSize) {
        long batchEnd = Math.min(confirmedHeight, batchStart + batchSize - 1L);
        log.debug(
            "开始处理确认块批次。chain={}, batchFrom={}, batchTo={}, checkpointBefore={}",
            profile.getChain(),
            batchStart,
            batchEnd,
            redisCheckpointService.getLastConfirmedBlock(profile.getChain()));
        log.debug(
            "开始处理确认块批次 ERC20 Transfer 日志。chain={}, batchFrom={}, batchTo={}",
            profile.getChain(),
            batchStart,
            batchEnd);
        for (ChainPaymentEvent event :
            erc20TransferScanService.scanConfirmedTransfers(profile.getChain(), batchStart, batchEnd)) {
          chainPaymentEventPublisher.publish(event);
        }
        subscriptionEventScanService.scan(profile.getChain(), batchStart, batchEnd);
        if (shouldScanNativeTransactions(profile.getChain())) {
          for (long blockNumber = batchStart; blockNumber <= batchEnd; blockNumber++) {
            EthBlock confirmedBlock =
                chainClient.getBlockByNumber(BigInteger.valueOf(blockNumber), true);
            if (confirmedBlock == null || confirmedBlock.getBlock() == null) {
              throw new IllegalStateException(
                  "Confirmed block is empty. chain=" + profile.getChain() + ", blockNumber=" + blockNumber);
            }
            log.debug(
                "已加载确认块。chain={}, blockNumber={}, txCount={}",
                profile.getChain(),
                blockNumber,
                confirmedBlock.getBlock().getTransactions() == null
                    ? 0
                    : confirmedBlock.getBlock().getTransactions().size());
            processConfirmedBlock(profile.getChain(), confirmedBlock.getBlock(), blockNumber);
          }
        } else {
          log.debug(
              "当前链未配置原生币收款，跳过全量区块交易扫描。chain={}, batchFrom={}, batchTo={}",
              profile.getChain(),
              batchStart,
              batchEnd);
        }
        if (batchEnd >= nextBlock) {
          redisCheckpointService.updateConfirmedBlock(profile.getChain(), batchEnd);
        }
      }
      redisCheckpointService.flushIfNeeded(profile.getChain(), false);
      scanUnconfirmedTransfers(profile.getChain(), confirmedHeight, latest.longValue(), confirmationDepth);
      clearChainFailure(profile.getChain());
    } catch (Exception ex) {
      markChainFailure(profile.getChain(), ex);
      alertService.alertScanFailure(profile.getChain(), ex.getMessage());
    }
  }

  private void scanUnconfirmedTransfers(
      String chain, long confirmedHeight, long latestBlock, int confirmationDepth) {
    if (latestBlock <= 0 || confirmationDepth <= 0) {
      return;
    }
    long fromBlock = Math.max(1L, confirmedHeight + 1L);
    long toBlock = latestBlock;
    if (fromBlock > toBlock) {
      return;
    }
    log.debug(
        "开始扫描最近未确认 ERC20 转账，用于收银台确认数展示。chain={}, fromBlock={}, toBlock={}, targetConfirmations={}",
        chain,
        fromBlock,
        toBlock,
        confirmationDepth);
    for (ChainPaymentEvent event : erc20TransferScanService.scanTransfers(chain, fromBlock, toBlock)) {
      pendingTransactionService.observe(event, confirmationDepth, latestBlock);
    }
  }

  private boolean isChainCoolingDown(String chain) {
    Instant until = chainCooldownUntil.get(chain.toUpperCase());
    if (until == null) {
      return false;
    }
    if (until.isAfter(Instant.now())) {
      log.debug("链扫描处于失败冷却期，本轮跳过。chain={}, retryAfter={}", chain, until);
      return true;
    }
    chainCooldownUntil.remove(chain.toUpperCase());
    return false;
  }

  private void markChainFailure(String chain, Exception ex) {
    Scanner scanner = properties.scannerForChain(chain);
    long cooldownSeconds = Math.max(1L, scanner.getFailureCooldownSeconds());
    Instant retryAfter = Instant.now().plusSeconds(cooldownSeconds);
    chainCooldownUntil.put(chain.toUpperCase(), retryAfter);
    if (isConfigurationOrProviderAccessError(ex)) {
      log.warn(
          "链扫描失败，疑似 RPC/节点配置或供应商权限问题，进入冷却期。chain={}, retryAfter={}, error={}",
          chain,
          retryAfter,
          ex.getMessage());
      log.debug("链扫描配置类异常堆栈。chain={}", chain, ex);
      return;
    }
    log.error(
        "链扫描失败，进入冷却期。chain={}, retryAfter={}, error={}",
        chain,
        retryAfter,
        ex.getMessage(),
        ex);
  }

  private void clearChainFailure(String chain) {
    chainCooldownUntil.remove(chain.toUpperCase());
  }

  private boolean isConfigurationOrProviderAccessError(Exception ex) {
    String message = ex.getMessage();
    return message != null
        && (message.contains("not enabled for this app")
            || message.contains("Invalid response received: 403")
            || message.contains("HTTP connect timed out")
            || message.contains("Failed to connect to WebSocket"));
  }

  private boolean shouldScanNativeTransactions(String chain) {
    return properties.getTokenProfiles().stream()
        .filter(Objects::nonNull)
        .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
        .anyMatch(profile -> !StringUtils.hasText(profile.getTokenAddress()));
  }

  private void processConfirmedBlock(String chain, EthBlock.Block block, long blockNumber) {
    if (block.getTransactions() == null || block.getTransactions().isEmpty()) {
      log.debug("确认块没有交易。chain={}, blockNumber={}", chain, blockNumber);
      return;
    }

    log.debug(
        "开始处理确认块交易。chain={}, blockNumber={}, txCount={}",
        chain,
        blockNumber,
        block.getTransactions().size());

    block
        .getTransactions()
        .forEach(
            txResult -> {
              try {
                var tx = (EthBlock.TransactionObject) txResult.get();
                if (tx == null || tx.getTo() == null) {
                  log.debug("交易对象为空或收款地址为空，跳过。chain={}, blockNumber={}", chain, blockNumber);
                  return;
                }

                if (tx.getValue() == null || tx.getValue().signum() <= 0) {
                  log.debug(
                      "跳过非原生币或零金额交易的原生币识别。chain={}, blockNumber={}, txHash={}",
                      chain,
                      blockNumber,
                      tx.getHash());
                  return;
                }

                BigDecimal amount =
                    new BigDecimal(tx.getValue())
                        .divide(new BigDecimal("1000000000000000000"), 6, RoundingMode.HALF_UP);

                chainPaymentEventPublisher.publish(
                    ChainPaymentEvent.of(
                        "EVM_NATIVE_BLOCK_SCAN",
                        chain,
                        null,
                        null,
                        tx.getHash(),
                        tx.getFrom(),
                        tx.getTo(),
                        amount,
                        blockNumber,
                        null,
                        block.getTimestamp() == null
                            ? null
                            : Instant.ofEpochSecond(block.getTimestamp().longValue())));
              } catch (Exception ex) {
                log.error(
                    "确认块单笔原生币交易处理失败，继续处理本块其它交易。chain={}, blockNumber={}, error={}",
                    chain,
                    blockNumber,
                    ex.getMessage(),
                    ex);
              }
            });
  }

}
