package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.manager.api.dto.ManagerDashboardOverviewResponse;
import io.swzxsyh.payment.callback.CallbackDeliveryStatus;
import io.swzxsyh.payment.domain.ContractSettlementStatus;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.exceptionorder.PaymentExceptionStatus;
import io.swzxsyh.payment.mapper.ChainScannerCheckpointMapper;
import io.swzxsyh.payment.mapper.ContractSettlementRecordMapper;
import io.swzxsyh.payment.mapper.DerivedAddressPoolRecordMapper;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.mapper.PaymentExceptionOrderMapper;
import io.swzxsyh.payment.mapper.PaymentOrderMapper;
import io.swzxsyh.payment.mapper.RawChainLogMapper;
import io.swzxsyh.payment.mapper.SubscriptionBillingRecordMapper;
import io.swzxsyh.payment.mapper.SubscriptionOrderMapper;
import io.swzxsyh.payment.persistence.entity.ContractSettlementRecord;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolRecord;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolStatus;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.persistence.entity.PaymentExceptionOrder;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionBillingStatus;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.SubscriptionStatus;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 管理端首页概览统计服务。 */
@Service
public class ManagerDashboardApplicationService {

  private final PaymentOrderMapper orderMapper;
  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final PaymentExceptionOrderMapper exceptionOrderMapper;
  private final RawChainLogMapper rawChainLogMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final ContractSettlementRecordMapper settlementMapper;
  private final ChainScannerCheckpointMapper scannerCheckpointMapper;
  private final DerivedAddressPoolRecordMapper addressPoolRecordMapper;
  private final SubscriptionOrderMapper subscriptionOrderMapper;
  private final SubscriptionBillingRecordMapper subscriptionBillingMapper;

  public ManagerDashboardApplicationService(
      PaymentOrderMapper orderMapper,
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      PaymentExceptionOrderMapper exceptionOrderMapper,
      RawChainLogMapper rawChainLogMapper,
      PaymentAuditRecordMapper auditMapper,
      ContractSettlementRecordMapper settlementMapper,
      ChainScannerCheckpointMapper scannerCheckpointMapper,
      DerivedAddressPoolRecordMapper addressPoolRecordMapper,
      SubscriptionOrderMapper subscriptionOrderMapper,
      SubscriptionBillingRecordMapper subscriptionBillingMapper) {
    this.orderMapper = orderMapper;
    this.callbackMapper = callbackMapper;
    this.exceptionOrderMapper = exceptionOrderMapper;
    this.rawChainLogMapper = rawChainLogMapper;
    this.auditMapper = auditMapper;
    this.settlementMapper = settlementMapper;
    this.scannerCheckpointMapper = scannerCheckpointMapper;
    this.addressPoolRecordMapper = addressPoolRecordMapper;
    this.subscriptionOrderMapper = subscriptionOrderMapper;
    this.subscriptionBillingMapper = subscriptionBillingMapper;
  }

  /** 查询管理端首页概览统计，用于状态卡片和告警入口。 */
  public ManagerDashboardOverviewResponse overview() {
    return new ManagerDashboardOverviewResponse(
        enumCounts(OrderStatus.class, status -> orderMapper.selectCount(
            Wrappers.<PaymentOrder>lambdaQuery().eq(PaymentOrder::getStatus, status))),
        enumCounts(CallbackDeliveryStatus.class, status -> callbackMapper.selectCount(
            Wrappers.<PaymentCallbackDeliveryRecord>lambdaQuery()
                .eq(PaymentCallbackDeliveryRecord::getStatus, status))),
        enumCounts(PaymentExceptionStatus.class, status -> exceptionOrderMapper.selectCount(
            Wrappers.<PaymentExceptionOrder>lambdaQuery()
                .eq(PaymentExceptionOrder::getStatus, status))),
        Map.of("RAW_CHAIN_LOG", rawChainLogMapper.selectCount(Wrappers.lambdaQuery())),
        enumCounts(ContractSettlementStatus.class, status -> settlementMapper.selectCount(
            Wrappers.<ContractSettlementRecord>lambdaQuery()
                .eq(ContractSettlementRecord::getStatus, status))),
        enumCounts(DerivedAddressPoolStatus.class, status -> addressPoolRecordMapper.selectCount(
            Wrappers.<DerivedAddressPoolRecord>lambdaQuery()
                .eq(DerivedAddressPoolRecord::getStatus, status))),
        enumCounts(SubscriptionStatus.class, status -> subscriptionOrderMapper.selectCount(
            Wrappers.<SubscriptionOrder>lambdaQuery().eq(SubscriptionOrder::getStatus, status))),
        enumCounts(SubscriptionBillingStatus.class, status -> subscriptionBillingMapper.selectCount(
            Wrappers.<SubscriptionBillingRecord>lambdaQuery()
                .eq(SubscriptionBillingRecord::getStatus, status))),
        scannerCheckpointMapper.selectCount(Wrappers.lambdaQuery()),
        auditMapper.selectCount(Wrappers.lambdaQuery()));
  }

  /** 按枚举值批量统计各状态数量。 */
  private <E extends Enum<E>> Map<String, Long> enumCounts(
      Class<E> enumClass, CountByStatus countByStatus) {
    Map<String, Long> counts = new LinkedHashMap<>();
    Arrays.stream(enumClass.getEnumConstants())
        .forEach(value -> counts.put(value.name(), countByStatus.count(value.name())));
    return counts;
  }

  @FunctionalInterface
  private interface CountByStatus {
    long count(String status);
  }
}
