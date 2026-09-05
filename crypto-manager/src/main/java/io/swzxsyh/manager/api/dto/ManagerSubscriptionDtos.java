package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import java.math.BigDecimal;
import java.util.List;

/** 订阅管理相关 DTO 聚合。 */
public final class ManagerSubscriptionDtos {

  private ManagerSubscriptionDtos() {}

  /** 订阅订单详情响应。 */
  public record DetailResponse(
      SubscriptionOrder order,
      List<SubscriptionBillingRecord> billings,
      List<PaymentCallbackDeliveryRecord> callbacks,
      List<PaymentAuditRecord> audits) {}

  /** 手动确认订阅账单请求。 */
  public record BillConfirmRequest(String txHash, BigDecimal realAmount, Long blockNumber) {}
}
