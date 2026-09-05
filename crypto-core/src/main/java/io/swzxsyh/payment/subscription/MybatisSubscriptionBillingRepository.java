package io.swzxsyh.payment.subscription;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.SubscriptionBillingRecordMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** 基于 MyBatis Plus 的订阅账单仓储实现。 */
@Repository
public class MybatisSubscriptionBillingRepository implements SubscriptionBillingRepository {

  private final SubscriptionBillingRecordMapper mapper;

  public MybatisSubscriptionBillingRepository(SubscriptionBillingRecordMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public SubscriptionBillingRecord save(SubscriptionBillingRecord record) {
    record.setUpdatedAt(LocalDateTime.now());
    if (record.getId() == null) {
      if (record.getCreatedAt() == null) {
        record.setCreatedAt(record.getUpdatedAt());
      }
      mapper.insert(record);
      return record;
    }
    mapper.updateById(record);
    return record;
  }

  @Override
  public boolean saveIfStatusIn(
      SubscriptionBillingRecord record, Set<SubscriptionBillingStatus> allowedStatuses) {
    if (record == null || record.getId() == null) {
      return false;
    }
    if (allowedStatuses == null || allowedStatuses.isEmpty()) {
      return false;
    }
    record.setUpdatedAt(LocalDateTime.now());
    int updated =
        mapper.update(
            record,
            new LambdaUpdateWrapper<SubscriptionBillingRecord>()
                .eq(SubscriptionBillingRecord::getId, record.getId())
                .in(SubscriptionBillingRecord::getStatus, allowedStatuses));
    return updated == 1;
  }

  @Override
  public Optional<SubscriptionBillingRecord> findLatestBySubscriptionOrderNo(
      String subscriptionOrderNo) {
    return Optional.ofNullable(mapper.selectOne(Wrappers.<SubscriptionBillingRecord>lambdaQuery()
        .eq(SubscriptionBillingRecord::getSubscriptionOrderNo, subscriptionOrderNo)
        .orderByDesc(SubscriptionBillingRecord::getBillingSequence)
        .last("LIMIT 1")));
  }

  @Override
  public Optional<SubscriptionBillingRecord> findByExecutionTxHash(String executionTxHash) {
    return Optional.ofNullable(mapper.selectOne(Wrappers.<SubscriptionBillingRecord>lambdaQuery()
        .eq(SubscriptionBillingRecord::getExecutionTxHash, executionTxHash)
        .last("LIMIT 1")));
  }

  @Override
  public List<SubscriptionBillingRecord> findExecutableBills(LocalDateTime now, int limit) {
    return mapper.selectList(Wrappers.<SubscriptionBillingRecord>lambdaQuery()
        .in(SubscriptionBillingRecord::getStatus,
            SubscriptionBillingStatus.PENDING, SubscriptionBillingStatus.FAILED)
        .and(wrapper -> wrapper.isNull(SubscriptionBillingRecord::getNextRetryAt)
            .or()
            .le(SubscriptionBillingRecord::getNextRetryAt, now))
        .orderByAsc(SubscriptionBillingRecord::getDueAt)
        .last("LIMIT " + Math.max(1, limit)));
  }

  @Override
  public List<SubscriptionBillingRecord> findExecutingBills(int limit) {
    return mapper.selectList(Wrappers.<SubscriptionBillingRecord>lambdaQuery()
        .eq(SubscriptionBillingRecord::getStatus, SubscriptionBillingStatus.EXECUTING)
        .isNotNull(SubscriptionBillingRecord::getExecutionTxHash)
        .orderByAsc(SubscriptionBillingRecord::getUpdatedAt)
        .last("LIMIT " + Math.max(1, limit)));
  }
}
