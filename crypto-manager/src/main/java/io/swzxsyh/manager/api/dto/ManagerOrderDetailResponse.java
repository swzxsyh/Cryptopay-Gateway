package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordView;
import java.util.List;

public record ManagerOrderDetailResponse(
    ManagerPaymentOrderView order,
    List<PaymentCallbackDeliveryRecord> callbacks,
    ContractSettlementRecordView settlement,
    List<PaymentAuditRecord> audits) {}
