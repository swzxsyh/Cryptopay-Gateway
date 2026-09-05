package io.swzxsyh.payment.scanner;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.config.CryptoPaymentProperties.TokenProfile;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.util.ArrayList;
import java.util.Collections;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.utils.Numeric;

/** ERC20 转账日志扫描服务。 */
@Slf4j
@Service
public class Erc20TransferScanService {

  private static final Event TRANSFER_EVENT =
      new Event(
          "Transfer",
          List.of(
              new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Address>(true) {},
              new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Address>(true) {},
              new org.web3j.abi.TypeReference<Uint256>() {}));
  private static final String TRANSFER_TOPIC = EventEncoder.encode(TRANSFER_EVENT);
  private static final int TOPIC_ADDRESS_BATCH_SIZE = 100;

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final ActiveDestinationAddressCache activeDestinationAddressCache;
  private final EvmBlockTimestampService blockTimestampService;
  private final LockUtil lockUtil;

  public Erc20TransferScanService(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      ActiveDestinationAddressCache activeDestinationAddressCache,
      EvmBlockTimestampService blockTimestampService,
      LockUtil lockUtil) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.activeDestinationAddressCache = activeDestinationAddressCache;
    this.blockTimestampService = blockTimestampService;
    this.lockUtil = lockUtil;
  }

  /** 扫描指定区块范围内的 ERC20 转账日志，只解析链上事实，不直接修改订单。 */
  public List<ChainPaymentEvent> scanConfirmedTransfers(String chain, long fromBlock, long toBlock) {
    return scanTransfers(chain, fromBlock, toBlock);
  }

  /** 扫描指定区块范围内的 ERC20 转账日志，调用方决定是作为待确认任务还是最终入账事件。 */
  public List<ChainPaymentEvent> scanTransfers(String chain, long fromBlock, long toBlock) {
    if (!StringUtils.hasText(chain) || fromBlock > toBlock) {
      return Collections.emptyList();
    }

    log.debug("开始扫描 ERC20 转账日志。chain={}, fromBlock={}, toBlock={}", chain, fromBlock, toBlock);
    String lockKey = RedisKeyNamespace.erc20ScannerLock(properties, chain);
    return lockUtil.withLock(
        lockKey,
        2000,
        10,
        () -> {
          List<ChainPaymentEvent> events = new ArrayList<>();
          ChainClient chainClient = chainClientFactory.get(chain);
          if (!chainClient.isEvmFamily()) {
            log.debug("非 EVM 链暂不执行 ERC20 扫描。chain={}, family={}, fromBlock={}, toBlock={}",
                chain, chainClient.family(), fromBlock, toBlock);
            return events;
          }
          List<TokenProfile> profiles =
              properties.getTokenProfiles().stream()
                  .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
                  .filter(profile -> StringUtils.hasText(profile.getTokenAddress()))
                  .filter(profile -> profile.getDecimals() > 0)
                  .sorted(
                      Comparator.comparing(
                          TokenProfile::getTokenAddress, String.CASE_INSENSITIVE_ORDER))
                  .toList();

          log.debug("ERC20 扫描配置已加载。chain={}, profileCount={}", chain, profiles.size());
          if (profiles.isEmpty()) {
            log.warn("当前链没有可用的 ERC20 配置，无法扫描代币转账。chain={}, fromBlock={}, toBlock={}",
                chain, fromBlock, toBlock);
          }
          long batchSize = Math.max(1L, properties.scannerForChain(chain).getLogScanBatchBlocks());
          for (long batchStart = fromBlock; batchStart <= toBlock; batchStart += batchSize) {
            long batchEnd = Math.min(toBlock, batchStart + batchSize - 1L);
            log.debug(
                "开始分段扫描 ERC20 Transfer 日志。chain={}, batchFrom={}, batchTo={}, tokenCount={}",
                chain,
                batchStart,
                batchEnd,
                profiles.size());
            for (TokenProfile profile : profiles) {
              events.addAll(scanProfile(chainClient, chain, profile, batchStart, batchEnd));
            }
          }
          return events;
        });
  }

  private List<ChainPaymentEvent> scanProfile(
      ChainClient chainClient, String chain, TokenProfile profile, long fromBlock, long toBlock) {
    List<ChainPaymentEvent> events = new ArrayList<>();
    Set<String> destinations =
        activeDestinationAddressCache.find(chain, profile.getTokenAddress());
    if (destinations.isEmpty()) {
      log.debug("当前链和代币没有活跃收款地址，跳过 ERC20 日志扫描。chain={}, token={}, tokenAddress={}, fromBlock={}, toBlock={}",
          chain, profile.getToken(), profile.getTokenAddress(), fromBlock, toBlock);
      return events;
    }

    List<String> destinationTopics =
        destinations.stream().map(this::addressToTopic).filter(StringUtils::hasText).toList();
    log.debug("ERC20 扫描使用活跃收款地址过滤。chain={}, token={}, tokenAddress={}, destinationCount={}",
        chain, profile.getToken(), profile.getTokenAddress(), destinationTopics.size());
    for (int index = 0; index < destinationTopics.size(); index += TOPIC_ADDRESS_BATCH_SIZE) {
      List<String> topicBatch =
          destinationTopics.subList(index, Math.min(destinationTopics.size(), index + TOPIC_ADDRESS_BATCH_SIZE));
      EthFilter filter =
          new EthFilter(
              new DefaultBlockParameterNumber(BigInteger.valueOf(fromBlock)),
              new DefaultBlockParameterNumber(BigInteger.valueOf(toBlock)),
              profile.getTokenAddress());
      filter.addSingleTopic(TRANSFER_TOPIC);
      filter.addNullTopic();
      filter.addOptionalTopics(topicBatch.toArray(String[]::new));

      EthLog logs = getLogsWithRetry(chainClient, chain, profile, filter, fromBlock, toBlock);
      if (logs == null || logs.getLogs() == null || logs.getLogs().isEmpty()) {
        log.debug("没有找到打到活跃收款地址的 ERC20 转账日志。chain={}, token={}, tokenAddress={}, fromBlock={}, toBlock={}, destinationBatchSize={}",
            chain, profile.getToken(), profile.getTokenAddress(), fromBlock, toBlock, topicBatch.size());
        continue;
      }
      log.info("找到打到活跃收款地址的 ERC20 转账日志。chain={}, token={}, tokenAddress={}, logCount={}, fromBlock={}, toBlock={}",
          chain, profile.getToken(), profile.getTokenAddress(), logs.getLogs().size(), fromBlock, toBlock);
      events.addAll(parseLogs(chain, profile, logs));
    }
    return events;
  }

  private List<ChainPaymentEvent> parseLogs(String chain, TokenProfile profile, EthLog logs) {
    List<ChainPaymentEvent> events = new ArrayList<>();
    for (EthLog.LogResult<?> logResult : logs.getLogs()) {
      Object value = logResult.get();
      if (!(value instanceof EthLog.LogObject logObject)) {
        continue;
      }
      if (!StringUtils.hasText(logObject.getTransactionHash())
          || logObject.getTopics() == null
          || logObject.getTopics().size() < 3) {
        log.debug("ERC20 日志格式不完整，跳过。chain={}, tokenAddress={}", chain, profile.getTokenAddress());
        continue;
      }

      String sourceAddress = topicToAddress(logObject.getTopics().get(1).toString());
      String destinationAddress = topicToAddress(logObject.getTopics().get(2).toString());
      BigDecimal amount = decodeAmount(logObject.getData(), profile.getDecimals());
      long blockNumber = logObject.getBlockNumber().longValue();
      java.time.Instant blockTimestamp =
          blockTimestampService.getBlockTimestamp(chain, blockNumber).orElse(null);
      log.debug("ERC20 转账候选已解析。chain={}, token={}, tokenAddress={}, txHash={}, from={}, to={}, amount={}, blockNumber={}",
          chain, profile.getToken(), profile.getTokenAddress(), logObject.getTransactionHash(), sourceAddress, destinationAddress, amount, blockNumber);
      events.add(
          ChainPaymentEvent.of(
              "EVM_ERC20_GET_LOGS",
              chain,
              profile.getToken(),
              profile.getTokenAddress(),
              logObject.getTransactionHash(),
              sourceAddress,
              destinationAddress,
              amount,
              blockNumber,
              logObject.getLogIndex() == null ? 0L : logObject.getLogIndex().longValue(),
              blockTimestamp));
      log.info(
          "ERC20 转账事件已解析待发布。chain={}, token={}, tokenAddress={}, txHash={}, to={}, amount={}, blockNumber={}",
          chain,
          profile.getToken(),
          profile.getTokenAddress(),
          logObject.getTransactionHash(),
          destinationAddress,
          amount,
          blockNumber);
    }
    return events;
  }

  private String addressToTopic(String address) {
    if (!StringUtils.hasText(address)) {
      return "";
    }
    String clean = Numeric.cleanHexPrefix(address.trim());
    if (clean.length() != 40) {
      log.debug("地址格式不是 EVM 20 字节地址，无法作为 ERC20 topic 过滤。address={}", address);
      return "";
    }
    return Numeric.prependHexPrefix("0".repeat(24) + clean.toLowerCase(Locale.ROOT));
  }

  private EthLog getLogsWithRetry(
      ChainClient chainClient,
      String chain,
      TokenProfile profile,
      EthFilter filter,
      long fromBlock,
      long toBlock) {
    int attempts = Math.max(1, properties.scannerForChain(chain).getLogScanRetryAttempts());
    RuntimeException lastException = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        return chainClient.getLogs(filter);
      } catch (RuntimeException ex) {
        lastException = ex;
        log.warn(
            "ERC20 日志扫描失败，准备重试。chain={}, token={}, tokenAddress={}, fromBlock={}, toBlock={}, attempt={}, maxAttempts={}, error={}",
            chain,
            profile.getToken(),
            profile.getTokenAddress(),
            fromBlock,
            toBlock,
            attempt,
            attempts,
            ex.getMessage());
        if (attempt < attempts) {
          sleepBeforeRetry(attempt);
        }
      }
    }
    throw new IllegalStateException(
        "ERC20 log scan failed after retries. chain="
            + chain
            + ", token="
            + profile.getToken()
            + ", fromBlock="
            + fromBlock
            + ", toBlock="
            + toBlock,
        lastException);
  }

  private void sleepBeforeRetry(int attempt) {
    try {
      Thread.sleep(Math.min(1000L * attempt, 3000L));
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("ERC20 log scan retry interrupted", ex);
    }
  }

  private String topicToAddress(String topic) {
    String clean = Numeric.cleanHexPrefix(topic);
    if (clean.length() <= 40) {
      return Numeric.prependHexPrefix(clean.substring(Math.max(0, clean.length() - 40)));
    }
    return Numeric.prependHexPrefix(clean.substring(clean.length() - 40));
  }

  private BigDecimal decodeAmount(String data, int decimals) {
    if (!StringUtils.hasText(data)) {
      return BigDecimal.ZERO;
    }
    BigInteger raw = Numeric.toBigInt(data);
    if (decimals <= 0) {
      return new BigDecimal(raw);
    }
    return new BigDecimal(raw).movePointLeft(decimals);
  }
}
