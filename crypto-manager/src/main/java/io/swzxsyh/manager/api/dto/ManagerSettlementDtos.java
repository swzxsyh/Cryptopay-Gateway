package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordView;
import java.util.List;

/** 合约结算管理相关 DTO 聚合。 */
public final class ManagerSettlementDtos {

  private ManagerSettlementDtos() {}

  /** 结算详情响应。 */
  public record DetailResponse(
      ContractSettlementRecordView settlement,
      List<PaymentCallbackDeliveryRecord> callbacks,
      List<PaymentAuditRecord> audits) {}

  /** 手动标记结算状态请求。 */
  public record MarkRequest(String txHash, Long blockNumber, String failureReason) {}
}
