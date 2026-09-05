package io.swzxsyh.payment.scanner;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.repository.PaymentOrderRepository;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 监听用活跃收款地址缓存。
 *
 * <p>这个缓存和 HD 地址池不是一回事：地址池保存“可租用/已租用地址”，这里保存“扫链时
 * 需要关注的到账地址”。扫描器优先读 Redis Set，Redis 缺失时回查数据库并回填，避免每次
 * eth_getLogs 前都从订单表聚合活跃地址。
 */
@Slf4j
@Service
public class ActiveDestinationAddressCache {

  private final CryptoPaymentProperties properties;
  private final ObjectProvider<RedisUtil> redisUtilProvider;
  private final PaymentOrderRepository orderRepository;

  public ActiveDestinationAddressCache(
      CryptoPaymentProperties properties,
      ObjectProvider<RedisUtil> redisUtilProvider,
      PaymentOrderRepository orderRepository) {
    this.properties = properties;
    this.redisUtilProvider = redisUtilProvider;
    this.orderRepository = orderRepository;
  }

  /** 注册订单当前的收款地址，让 watcher 后续可以从 Redis Set 获取监听目标。 */
  public void register(PaymentOrder order) {
    if (order == null || terminalWithoutLateScan(order.getStatus())) {
      return;
    }
    add(order.getChain(), order.getTokenAddress(), order.getPaymentAddress());
    add(order.getChain(), order.getTokenAddress(), order.getContractAddress());
  }

  /** 移除不再需要监听的订单收款地址。 */
  public void unregister(PaymentOrder order) {
    if (order == null) {
      return;
    }
    remove(order.getChain(), order.getTokenAddress(), order.getPaymentAddress());
    remove(order.getChain(), order.getTokenAddress(), order.getContractAddress());
  }

  /** 查询某个链和代币的活跃地址；Redis 未命中时使用数据库兜底并回填。 */
  public Set<String> find(String chain, String tokenAddress) {
    if (!StringUtils.hasText(chain)) {
      return Set.of();
    }
    RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
    if (redisUtil != null) {
      Set<String> cached = redisUtil.readStringSet(key(chain, tokenAddress));
      if (!cached.isEmpty()) {
        return cached;
      }
    }

    Set<String> databaseAddresses =
        new LinkedHashSet<>(orderRepository.findActiveDestinationAddresses(chain, tokenAddress));
    if (redisUtil != null && !databaseAddresses.isEmpty()) {
      String key = key(chain, tokenAddress);
      databaseAddresses.forEach(address -> redisUtil.addSetValue(key, normalizeAddress(address)));
      log.info("活跃收款地址 Redis 缓存为空，已从数据库回填。chain={}, tokenAddress={}, count={}",
          chain, tokenAddress, databaseAddresses.size());
    }
    return databaseAddresses;
  }

  private void add(String chain, String tokenAddress, String address) {
    RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
    if (redisUtil == null || !StringUtils.hasText(chain) || !StringUtils.hasText(address)) {
      return;
    }
    redisUtil.addSetValue(key(chain, tokenAddress), normalizeAddress(address));
  }

  private void remove(String chain, String tokenAddress, String address) {
    RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
    if (redisUtil == null || !StringUtils.hasText(chain) || !StringUtils.hasText(address)) {
      return;
    }
    redisUtil.removeSetValue(key(chain, tokenAddress), normalizeAddress(address));
  }

  private String key(String chain, String tokenAddress) {
    return RedisKeyNamespace.activeDestinationAddresses(properties, chain, tokenAddress);
  }

  private String normalizeAddress(String address) {
    return address.trim().toLowerCase(Locale.ROOT);
  }

  private boolean terminalWithoutLateScan(OrderStatus status) {
    return status == OrderStatus.PAID
        || status == OrderStatus.UNDERPAID
        || status == OrderStatus.OVERPAID
        || status == OrderStatus.CANCELLED;
  }
}
