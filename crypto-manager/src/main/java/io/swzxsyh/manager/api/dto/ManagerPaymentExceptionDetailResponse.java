package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentExceptionOrder;
import java.util.List;

/** 管理端异常单详情响应。 */
public record ManagerPaymentExceptionDetailResponse(
    PaymentExceptionOrder exceptionOrder,
    List<PaymentAuditRecord> audits) {}
