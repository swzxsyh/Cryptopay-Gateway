package io.swzxsyh.payment.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.mapper.PaymentOrderMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class MybatisPaymentOrderRepository implements PaymentOrderRepository {

  private final PaymentOrderMapper mapper;

  public MybatisPaymentOrderRepository(PaymentOrderMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public PaymentOrder save(PaymentOrder order) {
    order.setUpdatedAt(LocalDateTime.now());
    PaymentOrder current = mapper.selectById(order.getCryptoOrderNo());
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
  public boolean saveIfStatusIn(PaymentOrder order, Set<OrderStatus> allowedStatuses) {
    if (order == null || !StringUtils.hasText(order.getCryptoOrderNo())) {
      return false;
    }
    if (allowedStatuses == null || allowedStatuses.isEmpty()) {
      return false;
    }
    order.setUpdatedAt(LocalDateTime.now());
    int updated =
        mapper.update(
            order,
            new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getCryptoOrderNo, order.getCryptoOrderNo())
                .in(PaymentOrder::getStatus, allowedStatuses));
    return updated == 1;
  }

  @Override
  public boolean savePaymentResultIfClaimable(
      PaymentOrder order, Set<OrderStatus> allowedStatuses, String paymentTxHash) {
    if (order == null || !StringUtils.hasText(order.getCryptoOrderNo())) {
      return false;
    }
    if (allowedStatuses == null || allowedStatuses.isEmpty() || !StringUtils.hasText(paymentTxHash)) {
      return false;
    }
    String normalizedTxHash = paymentTxHash.trim();
    order.setUpdatedAt(LocalDateTime.now());
    int updated =
        mapper.update(
            order,
            new LambdaUpdateWrapper<PaymentOrder>()
                .eq(PaymentOrder::getCryptoOrderNo, order.getCryptoOrderNo())
                .in(PaymentOrder::getStatus, allowedStatuses)
                .and(
                    wrapper ->
                        wrapper
                            .isNull(PaymentOrder::getPaymentTxHash)
                            .or()
                            .eq(PaymentOrder::getPaymentTxHash, normalizedTxHash)));
    return updated == 1;
  }

  @Override
  public Optional<PaymentOrder> findByCryptoOrderNo(String cryptoOrderNo) {
    return Optional.ofNullable(mapper.selectById(cryptoOrderNo));
  }

  @Override
  public Optional<PaymentOrder> findByMerchantIdAndMerchantOrderNo(String merchantId, String merchantOrderNo) {
    if (!StringUtils.hasText(merchantId) || !StringUtils.hasText(merchantOrderNo)) {
      return Optional.empty();
    }
    return Optional.ofNullable(mapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
        .eq(PaymentOrder::getMerchantId, merchantId)
        .eq(PaymentOrder::getMerchantOrderNo, merchantOrderNo)));
  }

  @Override
  public Optional<PaymentOrder> findByPaymentTxHash(String paymentTxHash) {
    if (!StringUtils.hasText(paymentTxHash)) {
      return Optional.empty();
    }
    return Optional.ofNullable(mapper.selectOne(Wrappers.<PaymentOrder>lambdaQuery()
        .eq(PaymentOrder::getPaymentTxHash, paymentTxHash)));
  }

  @Override
  public List<PaymentOrder> findAll() {
    return mapper.selectList(null);
  }

  @Override
  public List<PaymentOrder> findActiveByChain(String chain) {
    if (!StringUtils.hasText(chain)) {
      return List.of();
    }
    return mapper.selectList(
        Wrappers.lambdaQuery(PaymentOrder.class)
            .eq(PaymentOrder::getChain, chain)
            .notIn(
                PaymentOrder::getStatus,
                OrderStatus.PAID,
                OrderStatus.UNDERPAID,
                OrderStatus.OVERPAID,
                OrderStatus.CANCELLED));
  }

  @Override
  public List<PaymentOrder> findExpirableOrders(LocalDateTime now, int limit) {
    return mapper.selectList(
        Wrappers.lambdaQuery(PaymentOrder.class)
            .isNotNull(PaymentOrder::getExpireTime)
            .le(PaymentOrder::getExpireTime, now)
            .in(
                PaymentOrder::getStatus,
                OrderStatus.CREATED,
                OrderStatus.METHOD_SELECTED,
                OrderStatus.WAITING_PAYMENT,
                OrderStatus.DETECTED)
            .orderByAsc(PaymentOrder::getExpireTime)
            .last("LIMIT " + Math.max(1, limit)));
  }

  @Override
  public List<PaymentOrder> findByChainAndDestinationAddress(String chain, String destinationAddress) {
    return findByChainAndDestinationAddressAndTokenAddress(chain, destinationAddress, null);
  }

  @Override
  public List<PaymentOrder> findByChainAndDestinationAddressAndTokenAddress(
      String chain, String destinationAddress, String tokenAddress) {
    if (!StringUtils.hasText(destinationAddress)) {
      return List.of();
    }
    List<PaymentOrder> candidates = new ArrayList<>();
    candidates.addAll(mapper.selectList(destinationQuery(chain, tokenAddress, PaymentOrder::getPaymentAddress, destinationAddress)));
    candidates.addAll(mapper.selectList(destinationQuery(chain, tokenAddress, PaymentOrder::getContractAddress, destinationAddress)));
    LinkedHashMap<String, PaymentOrder> deduplicated = new LinkedHashMap<>();
    for (PaymentOrder candidate : candidates) {
      if (StringUtils.hasText(candidate.getCryptoOrderNo())) {
        deduplicated.putIfAbsent(candidate.getCryptoOrderNo(), candidate);
      }
    }
    return List.copyOf(deduplicated.values());
  }

  @Override
  public Set<String> findActiveDestinationAddresses(String chain, String tokenAddress) {
    Set<String> addresses = new LinkedHashSet<>();
    List<PaymentOrder> orders = new ArrayList<>();
    orders.addAll(mapper.selectList(activeDestinationQuery(chain, tokenAddress, true)));
    orders.addAll(mapper.selectList(activeDestinationQuery(chain, tokenAddress, false)));
    for (PaymentOrder order : orders) {
      if (StringUtils.hasText(order.getPaymentAddress())) {
        addresses.add(order.getPaymentAddress().trim().toLowerCase());
      }
      if (StringUtils.hasText(order.getContractAddress())) {
        addresses.add(order.getContractAddress().trim().toLowerCase());
      }
    }
    return addresses;
  }

  @Override
  public List<PaymentOrder> findByDestinationAddress(String destinationAddress) {
    return findByChainAndDestinationAddress(null, destinationAddress);
  }

  @Override
  public List<PaymentOrder> findFinishedDerivedAddressLeases() {
    LambdaQueryWrapper<PaymentOrder> query = Wrappers.lambdaQuery(PaymentOrder.class)
        .eq(PaymentOrder::getPaymentMethod, PaymentMethod.DERIVED_ADDRESS)
        .in(
            PaymentOrder::getStatus,
            OrderStatus.PAID,
            OrderStatus.OVERPAID,
            OrderStatus.CANCELLED,
            OrderStatus.EXPIRED)
        .isNotNull(PaymentOrder::getDerivedAddressPoolKey)
        .isNotNull(PaymentOrder::getDerivedAddressLeaseId);
    return mapper.selectList(query);
  }

  private LambdaQueryWrapper<PaymentOrder> destinationQuery(
      String chain,
      String tokenAddress,
      com.baomidou.mybatisplus.core.toolkit.support.SFunction<PaymentOrder, ?> addressColumn,
      String destinationAddress) {
    return Wrappers.lambdaQuery(PaymentOrder.class)
        .eq(StringUtils.hasText(chain), PaymentOrder::getChain, chain)
        .eq(StringUtils.hasText(tokenAddress), PaymentOrder::getTokenAddress, tokenAddress)
        .eq(addressColumn, destinationAddress);
  }

  private LambdaQueryWrapper<PaymentOrder> activeDestinationQuery(
      String chain, String tokenAddress, boolean paymentAddress) {
    LambdaQueryWrapper<PaymentOrder> query = Wrappers.lambdaQuery(PaymentOrder.class)
        .eq(StringUtils.hasText(chain), PaymentOrder::getChain, chain)
        .eq(StringUtils.hasText(tokenAddress), PaymentOrder::getTokenAddress, tokenAddress)
        .notIn(
            PaymentOrder::getStatus,
            OrderStatus.PAID,
            OrderStatus.UNDERPAID,
            OrderStatus.OVERPAID,
            OrderStatus.CANCELLED);
    return paymentAddress
        ? query.isNotNull(PaymentOrder::getPaymentAddress)
        : query.isNotNull(PaymentOrder::getContractAddress);
  }
}
