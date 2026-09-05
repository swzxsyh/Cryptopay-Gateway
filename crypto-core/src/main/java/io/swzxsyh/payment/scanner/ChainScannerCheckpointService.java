package io.swzxsyh.payment.scanner;

import io.swzxsyh.payment.mapper.ChainScannerCheckpointMapper;
import io.swzxsyh.payment.persistence.entity.ChainScannerCheckpoint;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.dao.DuplicateKeyException;

/** 链扫描 checkpoint 管理服务，用于记录扫描进度与确认进度。 */
@Slf4j
@Service
public class ChainScannerCheckpointService {

  private final ChainScannerCheckpointMapper mapper;

  public ChainScannerCheckpointService(ChainScannerCheckpointMapper mapper) {
    this.mapper = mapper;
  }

  /** 获取或创建某条链的扫描 checkpoint。 */
  public ChainScannerCheckpoint getOrCreate(String chain) {
    String normalizedChain = normalize(chain);
    ChainScannerCheckpoint checkpoint = mapper.selectById(normalizedChain);
    if (checkpoint != null) {
      return checkpoint;
    }

    checkpoint = new ChainScannerCheckpoint();
    checkpoint.setChain(normalizedChain);
    checkpoint.setLatestObservedBlock(0L);
    checkpoint.setLastConfirmedBlock(0L);
    checkpoint.setCreatedAt(LocalDateTime.now());
    checkpoint.setUpdatedAt(checkpoint.getCreatedAt());
    try {
      mapper.insert(checkpoint);
      return checkpoint;
    } catch (DuplicateKeyException ex) {
      ChainScannerCheckpoint existing = mapper.selectById(normalizedChain);
      if (existing != null) {
        return existing;
      }
      throw ex;
    }
  }

  /** 更新链上最新已观察到的区块高度。 */
  public ChainScannerCheckpoint updateObservedBlock(String chain, long latestObservedBlock) {
    ChainScannerCheckpoint checkpoint = getOrCreate(chain);
    if (latestObservedBlock > checkpoint.getLatestObservedBlock()) {
      checkpoint.setLatestObservedBlock(latestObservedBlock);
      checkpoint.setUpdatedAt(LocalDateTime.now());
      mapper.updateById(checkpoint);
    }
    return checkpoint;
  }

  /** 更新链上最新已确认的区块高度。 */
  public ChainScannerCheckpoint updateConfirmedBlock(String chain, long lastConfirmedBlock) {
    ChainScannerCheckpoint checkpoint = getOrCreate(chain);
    if (lastConfirmedBlock > checkpoint.getLastConfirmedBlock()) {
      checkpoint.setLastConfirmedBlock(lastConfirmedBlock);
      checkpoint.setUpdatedAt(LocalDateTime.now());
      mapper.updateById(checkpoint);
    }
    return checkpoint;
  }

  /** 统一规范链名格式，避免同链多写法。 */
  private String normalize(String chain) {
    if (!StringUtils.hasText(chain)) {
      throw new IllegalArgumentException("chain is required");
    }
    return chain.trim().toUpperCase();
  }
}
