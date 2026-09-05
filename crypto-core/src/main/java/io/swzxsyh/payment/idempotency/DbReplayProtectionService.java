package io.swzxsyh.payment.idempotency;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.mapper.ReplayTransactionRecordMapper;
import io.swzxsyh.payment.persistence.entity.ReplayTransactionRecord;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class DbReplayProtectionService {

  private final ReplayTransactionRecordMapper mapper;
  private final LockUtil lockUtil;
  private final CryptoPaymentProperties properties;

  public DbReplayProtectionService(
      ReplayTransactionRecordMapper mapper,
      LockUtil lockUtil,
      CryptoPaymentProperties properties) {
    this.mapper = mapper;
    this.lockUtil = lockUtil;
    this.properties = properties;
  }

  public boolean claimTx(String chain, String txHash, long ttlHours) {
    if (!StringUtils.hasText(chain) || !StringUtils.hasText(txHash)) {
      return false;
    }
    String normalizedChain = chain.trim().toUpperCase();
    String normalizedHash = txHash.trim().toLowerCase();
    String lockKey = RedisKeyNamespace.dbReplayLock(properties, normalizedChain, normalizedHash);
    long safeTtlHours = ttlHours > 0 ? ttlHours : 72L;

    return lockUtil.withLock(lockKey, 2000, 8, () -> {
      LocalDateTime now = LocalDateTime.now();
      ReplayTransactionRecord existing = mapper.selectOne(
          new LambdaQueryWrapper<ReplayTransactionRecord>()
              .eq(ReplayTransactionRecord::getChain, normalizedChain)
              .eq(ReplayTransactionRecord::getTxHash, normalizedHash)
      );
      if (existing != null
          && (existing.getExpiresAt() == null || existing.getExpiresAt().isBefore(now))) {
        mapper.deleteById(existing.getId());
        existing = null;
      }
      if (existing != null) {
        return false;
      }

      ReplayTransactionRecord record = new ReplayTransactionRecord();
      record.setChain(normalizedChain);
      record.setTxHash(normalizedHash);
      record.setExpiresAt(now.plusHours(safeTtlHours));
      record.setCreatedAt(now);
      try {
        mapper.insert(record);
      } catch (DuplicateKeyException ex) {
        log.debug(
            "交易防重放记录已被其它节点抢先写入。chain={}, txHash={}",
            normalizedChain,
            normalizedHash);
        return false;
      }
      log.debug(
          "已写入交易防重放记录。chain={}, txHash={}, expiresAt={}",
          normalizedChain,
          normalizedHash,
          record.getExpiresAt());
      return true;
    });
  }

  /** 定时清理过期的链上交易防重放记录。 */
  @Scheduled(fixedDelay = 3600000)
  public void purgeExpiredRecords() {
    LocalDateTime now = LocalDateTime.now();
    int deleted =
        mapper.delete(
            new LambdaQueryWrapper<ReplayTransactionRecord>()
                .isNotNull(ReplayTransactionRecord::getExpiresAt)
                .lt(ReplayTransactionRecord::getExpiresAt, now));
    if (deleted > 0) {
      log.info("清理过期交易防重放记录完成。deletedCount={}", deleted);
    }
  }
}
