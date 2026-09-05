package io.swzxsyh.payment.subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 订阅账单仓储接口。 */
public interface SubscriptionBillingRepository {

  /** 保存或更新账单。 */
  SubscriptionBillingRecord save(SubscriptionBillingRecord record);

  /** 仅当当前状态仍在允许集合内时更新账单，避免重复确认或失败覆盖成功。 */
  boolean saveIfStatusIn(SubscriptionBillingRecord record, Set<SubscriptionBillingStatus> allowedStatuses);

  /** 查询订阅订单下的最近一期账单。 */
  Optional<SubscriptionBillingRecord> findLatestBySubscriptionOrderNo(String subscriptionOrderNo);

  /** 按交易哈希查询账单。 */
  Optional<SubscriptionBillingRecord> findByExecutionTxHash(String executionTxHash);

  /** 查询待执行或待重试账单。 */
  List<SubscriptionBillingRecord> findExecutableBills(LocalDateTime now, int limit);

  /** 查询等待链上确认的账单。 */
  List<SubscriptionBillingRecord> findExecutingBills(int limit);
}
