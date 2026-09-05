package io.swzxsyh.payment.scanner;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** EVM 区块时间查询服务；按链和区块号做本地缓存，避免 ERC20 多日志重复拉同一块。 */
@Slf4j
@Service
public class EvmBlockTimestampService {

  private final ChainClientFactory chainClientFactory;
  private final Map<String, Instant> cache = new ConcurrentHashMap<>();

  public EvmBlockTimestampService(ChainClientFactory chainClientFactory) {
    this.chainClientFactory = chainClientFactory;
  }

  /** 获取 EVM 区块真实时间戳。 */
  public Optional<Instant> getBlockTimestamp(String chain, long blockNumber) {
    if (!StringUtils.hasText(chain) || blockNumber <= 0L) {
      return Optional.empty();
    }
    String key = chain.trim().toUpperCase() + ":" + blockNumber;
    Instant cached = cache.get(key);
    if (cached != null) {
      return Optional.of(cached);
    }
    try {
      ChainClient chainClient = chainClientFactory.get(chain);
      if (!chainClient.isEvmFamily()) {
        return Optional.empty();
      }
      var block = chainClient.getBlockByNumber(BigInteger.valueOf(blockNumber), false);
      if (block == null || block.getBlock() == null || block.getBlock().getTimestamp() == null) {
        return Optional.empty();
      }
      Instant timestamp = Instant.ofEpochSecond(block.getBlock().getTimestamp().longValue());
      cache.put(key, timestamp);
      return Optional.of(timestamp);
    } catch (Exception ex) {
      log.warn("获取 EVM 区块时间失败，将使用扫描观察时间兜底。chain={}, blockNumber={}, error={}",
          chain, blockNumber, ex.getMessage());
      return Optional.empty();
    }
  }
}
