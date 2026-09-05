package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerSubscriptionDtos.BillConfirmRequest;
import io.swzxsyh.manager.api.dto.ManagerSubscriptionDtos.DetailResponse;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.mapper.SubscriptionBillingRecordMapper;
import io.swzxsyh.payment.mapper.SubscriptionOrderMapper;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionBillingService;
import io.swzxsyh.payment.subscription.SubscriptionBillingStatus;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.SubscriptionOrderService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端订阅订单、订阅账单和人工操作应用服务。 */
@Service
public class ManagerSubscriptionApplicationService extends ManagerApplicationSupport {

  private final SubscriptionOrderMapper subscriptionOrderMapper;
  private final SubscriptionBillingRecordMapper subscriptionBillingMapper;
  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final SubscriptionOrderService subscriptionOrderService;
  private final SubscriptionBillingService subscriptionBillingService;

  public ManagerSubscriptionApplicationService(
      SubscriptionOrderMapper subscriptionOrderMapper,
      SubscriptionBillingRecordMapper subscriptionBillingMapper,
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      PaymentAuditRecordMapper auditMapper,
      SubscriptionOrderService subscriptionOrderService,
      SubscriptionBillingService subscriptionBillingService) {
    this.subscriptionOrderMapper = subscriptionOrderMapper;
    this.subscriptionBillingMapper = subscriptionBillingMapper;
    this.callbackMapper = callbackMapper;
    this.auditMapper = auditMapper;
    this.subscriptionOrderService = subscriptionOrderService;
    this.subscriptionBillingService = subscriptionBillingService;
  }

  /** 分页查询订阅订单，用于订阅生命周期管理页面。 */
  public ManagerPageResponse<SubscriptionOrder> pageSubscriptionOrders(
      long page, long size, String merchantId, String subscriptionOrderNo, String status) {
    LambdaQueryWrapper<SubscriptionOrder> query = Wrappers.<SubscriptionOrder>lambdaQuery()
        .eq(hasText(subscriptionOrderNo), SubscriptionOrder::getSubscriptionOrderNo, subscriptionOrderNo)
        .eq(hasText(status), SubscriptionOrder::getStatus, normalizeFilterCode(status));
    applyMerchantDataScope(query, SubscriptionOrder::getMerchantId, merchantId);
    query.orderByDesc(SubscriptionOrder::getCreatedAt);
    return page(subscriptionOrderMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 查询订阅详情，并组合账单、回调和审计记录。 */
  public DetailResponse subscriptionDetail(String subscriptionOrderNo) {
    SubscriptionOrder order =
        require(subscriptionOrderMapper.selectById(subscriptionOrderNo), "subscription order not found");
    requireMerchantDataAccess(order.getMerchantId());
    java.util.List<SubscriptionBillingRecord> billings = subscriptionBillingMapper.selectList(
        Wrappers.<SubscriptionBillingRecord>lambdaQuery()
            .eq(SubscriptionBillingRecord::getSubscriptionOrderNo, subscriptionOrderNo)
            .orderByDesc(SubscriptionBillingRecord::getBillingSequence));
    return new DetailResponse(
        order,
        billings,
        callbacksByBizOrderNo(callbackMapper, subscriptionOrderNo),
        audits(auditMapper, "SUBSCRIPTION_ORDER", subscriptionOrderNo));
  }

  /** 人工暂停订阅，复用 core 订阅服务执行状态流转。 */
  public Object pauseSubscription(String subscriptionOrderNo) {
    requireSubscriptionOrderAccess(subscriptionOrderNo);
    return subscriptionOrderService.pause(subscriptionOrderNo);
  }

  /** 人工恢复订阅，复用 core 订阅服务执行状态流转。 */
  public Object resumeSubscription(String subscriptionOrderNo) {
    requireSubscriptionOrderAccess(subscriptionOrderNo);
    return subscriptionOrderService.resume(subscriptionOrderNo);
  }

  /** 人工取消订阅，复用 core 订阅服务执行状态流转。 */
  public Object cancelSubscription(String subscriptionOrderNo) {
    requireSubscriptionOrderAccess(subscriptionOrderNo);
    return subscriptionOrderService.cancel(subscriptionOrderNo);
  }

  /** 分页查询订阅账单，用于扣款、补偿和争议处理页面。 */
  public ManagerPageResponse<SubscriptionBillingRecord> pageSubscriptionBillings(
      long page, long size, String merchantId, String subscriptionOrderNo, String status) {
    LambdaQueryWrapper<SubscriptionBillingRecord> query =
        Wrappers.<SubscriptionBillingRecord>lambdaQuery()
            .eq(hasText(subscriptionOrderNo), SubscriptionBillingRecord::getSubscriptionOrderNo, subscriptionOrderNo)
            .eq(hasText(status), SubscriptionBillingRecord::getStatus, normalizeFilterCode(status));
    applyMerchantDataScope(query, SubscriptionBillingRecord::getMerchantId, merchantId);
    query.orderByDesc(SubscriptionBillingRecord::getCreatedAt);
    return page(subscriptionBillingMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 人工确认订阅账单已支付，并触发 core 账单确认逻辑。 */
  @Transactional
  public SubscriptionBillingRecord confirmSubscriptionBilling(
      Long billingId, BillConfirmRequest request) {
    SubscriptionBillingRecord bill =
        require(subscriptionBillingMapper.selectById(billingId), "subscription billing not found");
    requireMerchantDataAccess(bill.getMerchantId());
    BigDecimal realAmount = request == null ? null : request.realAmount();
    String txHash = request == null ? bill.getExecutionTxHash() : request.txHash();
    Long blockNumber = request == null ? null : request.blockNumber();
    return subscriptionBillingService.confirmPaid(bill, txHash, realAmount, blockNumber);
  }

  /** 人工关闭订阅账单，停止后续自动重试。 */
  @Transactional
  public SubscriptionBillingRecord cancelSubscriptionBilling(Long billingId, String reason) {
    SubscriptionBillingRecord bill =
        require(subscriptionBillingMapper.selectById(billingId), "subscription billing not found");
    requireMerchantDataAccess(bill.getMerchantId());
    bill.setStatus(SubscriptionBillingStatus.CANCELLED);
    bill.setFailureReason(reason);
    bill.setNextRetryAt(null);
    bill.setUpdatedAt(LocalDateTime.now());
    int updated = subscriptionBillingMapper.update(
        bill,
        Wrappers.<SubscriptionBillingRecord>lambdaUpdate()
            .eq(SubscriptionBillingRecord::getId, billingId)
            .in(SubscriptionBillingRecord::getStatus,
                Set.of(SubscriptionBillingStatus.PENDING, SubscriptionBillingStatus.FAILED)));
    if (updated != 1) {
      throw new IllegalStateException("subscription billing status changed concurrently: " + billingId);
    }
    return bill;
  }

  private void requireSubscriptionOrderAccess(String subscriptionOrderNo) {
    SubscriptionOrder order =
        require(subscriptionOrderMapper.selectById(subscriptionOrderNo), "subscription order not found");
    requireMerchantDataAccess(order.getMerchantId());
  }
}
