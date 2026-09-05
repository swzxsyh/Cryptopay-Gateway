package io.swzxsyh.payment.rawchain;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.RawChainLogMapper;
import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.persistence.entity.RawChainLog;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 原始链上流水服务，负责链上事实落库、匹配状态更新和人工处理记录。 */
@Slf4j
@Service
public class RawChainLogService {

  private final RawChainLogMapper rawChainLogMapper;

  public RawChainLogService(RawChainLogMapper rawChainLogMapper) {
    this.rawChainLogMapper = rawChainLogMapper;
  }

  /** 先把链上事件以 UNMATCHED 状态落库；重复扫描到同一日志时返回已有记录。 */
  @Transactional
  public RawChainLog recordObserved(ChainPaymentEvent event) {
    if (event == null || !StringUtils.hasText(event.chain()) || !StringUtils.hasText(event.txHash())) {
      throw new IllegalArgumentException("chain and txHash are required for raw chain log");
    }
    Optional<RawChainLog> existing = find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex()));
    if (existing.isPresent()) {
      return existing.get();
    }

    LocalDateTime now = LocalDateTime.now();
    RawChainLog rawLog = new RawChainLog();
    rawLog.setChain(event.chain());
    rawLog.setTxHash(event.txHash());
    rawLog.setLogIndex(normalizeLogIndex(event.logIndex()));
    rawLog.setSource(event.source());
    rawLog.setToken(event.token());
    rawLog.setTokenAddress(event.tokenAddress());
    rawLog.setFromAddress(event.sourceAddress());
    rawLog.setToAddress(event.destinationAddress());
    rawLog.setAmount(event.amount());
    rawLog.setBlockNumber(event.blockNumber());
    rawLog.setBlockTimestamp(resolveBlockTimestamp(event, now));
    rawLog.setStatus(RawChainLogStatus.UNMATCHED.name());
    rawLog.setObservedAt(now);
    rawLog.setCreatedAt(now);
    rawLog.setUpdatedAt(now);
    try {
      rawChainLogMapper.insert(rawLog);
      log.info(
          "原始链上流水已记录。chain={}, txHash={}, logIndex={}, to={}, amount={}, status={}",
          rawLog.getChain(),
          rawLog.getTxHash(),
          rawLog.getLogIndex(),
          rawLog.getToAddress(),
          rawLog.getAmount(),
          rawLog.getStatus());
      return rawLog;
    } catch (DuplicateKeyException ex) {
      return find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex()))
          .orElseThrow(() -> ex);
    }
  }

  private LocalDateTime resolveBlockTimestamp(ChainPaymentEvent event, LocalDateTime fallbackTime) {
    if (event.blockTimestamp() != null) {
      return LocalDateTime.ofInstant(event.blockTimestamp(), ZoneId.systemDefault());
    }
    return event.observedAt() == null
        ? fallbackTime
        : LocalDateTime.ofInstant(event.observedAt(), ZoneId.systemDefault());
  }

  /** 标记原始流水已匹配订单。 */
  @Transactional
  public void markMatched(ChainPaymentEvent event, String cryptoOrderNo) {
    RawChainLog rawLog =
        find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex()))
            .orElseGet(() -> recordObserved(event));
    if (RawChainLogStatus.MANUAL_PROCESSED.name().equals(rawLog.getStatus())) {
      return;
    }
    rawLog.setStatus(RawChainLogStatus.MATCHED.name());
    rawLog.setMatchedCryptoOrderNo(cryptoOrderNo);
    rawLog.setMatchReason("自动匹配到支付订单");
    rawLog.setUpdatedAt(LocalDateTime.now());
    rawChainLogMapper.updateById(rawLog);
  }

  /** 标记原始流水未匹配订单，保留给运营查询。 */
  @Transactional
  public void markUnmatched(ChainPaymentEvent event, String reason) {
    RawChainLog rawLog =
        find(event.chain(), event.txHash(), normalizeLogIndex(event.logIndex()))
            .orElseGet(() -> recordObserved(event));
    if (RawChainLogStatus.MANUAL_PROCESSED.name().equals(rawLog.getStatus())
        || RawChainLogStatus.MATCHED.name().equals(rawLog.getStatus())) {
      return;
    }
    rawLog.setStatus(RawChainLogStatus.UNMATCHED.name());
    rawLog.setMatchReason(reason);
    rawLog.setUpdatedAt(LocalDateTime.now());
    rawChainLogMapper.updateById(rawLog);
  }

  /** 运营人工处理原始流水。 */
  @Transactional
  public RawChainLog markManualProcessed(Long id, String operator, String note) {
    RawChainLog rawLog = rawChainLogMapper.selectById(id);
    if (rawLog == null) {
      throw new IllegalArgumentException("raw chain log not found");
    }
    rawLog.setStatus(RawChainLogStatus.MANUAL_PROCESSED.name());
    rawLog.setOperator(operator);
    rawLog.setOperatorNote(note);
    rawLog.setManualProcessedAt(LocalDateTime.now());
    rawLog.setUpdatedAt(LocalDateTime.now());
    rawChainLogMapper.updateById(rawLog);
    return rawLog;
  }

  private Optional<RawChainLog> find(String chain, String txHash, Long logIndex) {
    return Optional.ofNullable(
        rawChainLogMapper.selectOne(
            Wrappers.<RawChainLog>lambdaQuery()
                .eq(RawChainLog::getChain, chain)
                .eq(RawChainLog::getTxHash, txHash)
                .eq(RawChainLog::getLogIndex, logIndex)
                .last("LIMIT 1")));
  }

  private Long normalizeLogIndex(Long logIndex) {
    return logIndex == null || logIndex < 0 ? 0L : logIndex;
  }
}
