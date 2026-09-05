package io.swzxsyh.payment.subscription;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 订阅订单仓储接口，屏蔽底层数据库实现。 */
public interface SubscriptionOrderRepository {

  /** 保存或更新订阅订单。 */
  SubscriptionOrder save(SubscriptionOrder order);

  /** 仅当当前状态仍在允许集合内时更新订阅订单，用于多节点下的状态防回退。 */
  boolean saveIfStatusIn(SubscriptionOrder order, Set<SubscriptionStatus> allowedStatuses);

  /** 按订阅单号查询订单。 */
  Optional<SubscriptionOrder> findBySubscriptionOrderNo(String subscriptionOrderNo);

  /** 按初始化交易哈希查询订单。 */
  Optional<SubscriptionOrder> findBySetupTxHash(String setupTxHash);

  /** 按链上订阅事件 ID 查询订单。 */
  Optional<SubscriptionOrder> findBySubscriptionEventId(String subscriptionEventId);

  /** 查询已经激活且到达扣款时间的订阅。 */
  List<SubscriptionOrder> findDueActiveOrders(java.time.LocalDateTime now, int limit);

  /** 查询等待链上初始化回执确认的订阅。 */
  List<SubscriptionOrder> findActivatingOrdersWithSetupTx(int limit);

  /** 查询全部订阅订单。 */
  List<SubscriptionOrder> findAll();
}
