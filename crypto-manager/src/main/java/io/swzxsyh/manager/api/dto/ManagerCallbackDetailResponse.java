package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import java.util.List;

public record ManagerCallbackDetailResponse(
    PaymentCallbackDeliveryRecord callback, List<PaymentAuditRecord> audits) {}
