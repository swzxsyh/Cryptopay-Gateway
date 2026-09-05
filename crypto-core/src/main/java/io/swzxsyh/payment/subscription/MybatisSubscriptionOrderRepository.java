package io.swzxsyh.payment.subscription;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.SubscriptionOrderMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
/** 基于 MyBatis Plus 的订阅订单仓储实现。 */
public class MybatisSubscriptionOrderRepository implements SubscriptionOrderRepository {

  private final SubscriptionOrderMapper mapper;

  public MybatisSubscriptionOrderRepository(SubscriptionOrderMapper mapper) {
    this.mapper = mapper;
  }

  /** 保存或更新订阅订单。 */
  @Override
  public SubscriptionOrder save(SubscriptionOrder order) {
    order.setUpdatedAt(LocalDateTime.now());
    SubscriptionOrder current = mapper.selectById(order.getSubscriptionOrderNo());
    if (current == null) {
      if (order.getCreatedAt() == null) {
        order.setCreatedAt(order.getUpdatedAt());
      }
      mapper.insert(order);
      return order;
    }
    mapper.updateById(order);
    return order;
  }

  @Override
  public boolean saveIfStatusIn(SubscriptionOrder order, Set<SubscriptionStatus> allowedStatuses) {
    if (order == null || !StringUtils.hasText(order.getSubscriptionOrderNo())) {
      return false;
    }
    if (allowedStatuses == null || allowedStatuses.isEmpty()) {
      return false;
    }
    order.setUpdatedAt(LocalDateTime.now());
    int updated =
        mapper.update(
            order,
            new LambdaUpdateWrapper<SubscriptionOrder>()
                .eq(SubscriptionOrder::getSubscriptionOrderNo, order.getSubscriptionOrderNo())
                .in(SubscriptionOrder::getStatus, allowedStatuses));
    return updated == 1;
  }

  /** 按订阅单号查询订单。 */
  @Override
  public Optional<SubscriptionOrder> findBySubscriptionOrderNo(String subscriptionOrderNo) {
    return Optional.ofNullable(mapper.selectById(subscriptionOrderNo));
  }

  /** 按初始化交易哈希查询订单。 */
  @Override
  public Optional<SubscriptionOrder> findBySetupTxHash(String setupTxHash) {
    return Optional.ofNullable(mapper.selectOne(Wrappers.<SubscriptionOrder>lambdaQuery()
        .eq(SubscriptionOrder::getSetupTxHash, setupTxHash)
        .last("LIMIT 1")));
  }

  /** 按链上订阅事件 ID 查询订单。 */
  @Override
  public Optional<SubscriptionOrder> findBySubscriptionEventId(String subscriptionEventId) {
    return Optional.ofNullable(mapper.selectOne(Wrappers.<SubscriptionOrder>lambdaQuery()
        .eq(SubscriptionOrder::getSubscriptionEventId, SubscriptionEventId.normalize(subscriptionEventId))
        .last("LIMIT 1")));
  }

  /** 查询已经激活且到达扣款时间的订阅。 */
  @Override
  public List<SubscriptionOrder> findDueActiveOrders(LocalDateTime now, int limit) {
    return mapper.selectList(Wrappers.<SubscriptionOrder>lambdaQuery()
        .eq(SubscriptionOrder::getStatus, SubscriptionStatus.ACTIVE)
        .le(SubscriptionOrder::getNextBillingAt, now)
        .orderByAsc(SubscriptionOrder::getNextBillingAt)
        .last("LIMIT " + Math.max(1, limit)));
  }

  /** 查询等待链上初始化回执确认的订阅。 */
  @Override
  public List<SubscriptionOrder> findActivatingOrdersWithSetupTx(int limit) {
    return mapper.selectList(Wrappers.<SubscriptionOrder>lambdaQuery()
        .eq(SubscriptionOrder::getStatus, SubscriptionStatus.ACTIVATING)
        .isNotNull(SubscriptionOrder::getSetupTxHash)
        .ne(SubscriptionOrder::getSetupTxHash, "")
        .orderByAsc(SubscriptionOrder::getUpdatedAt)
        .last("LIMIT " + Math.max(1, limit)));
  }

  /** 查询全部订阅订单。 */
  @Override
  public List<SubscriptionOrder> findAll() {
    return mapper.selectList(null);
  }
}
