package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerPaymentExceptionDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerPaymentExceptionHandleRequest;
import io.swzxsyh.payment.exceptionorder.PaymentExceptionOrderService;
import io.swzxsyh.payment.exceptionorder.PaymentExceptionStatus;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentExceptionOrderMapper;
import io.swzxsyh.payment.persistence.entity.PaymentExceptionOrder;
import org.springframework.stereotype.Service;

/** 管理端支付异常单查询与人工处理服务。 */
@Service
public class ManagerPaymentExceptionApplicationService extends ManagerApplicationSupport {

  private final PaymentExceptionOrderMapper exceptionOrderMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final PaymentExceptionOrderService exceptionOrderService;

  public ManagerPaymentExceptionApplicationService(
      PaymentExceptionOrderMapper exceptionOrderMapper,
      PaymentAuditRecordMapper auditMapper,
      PaymentExceptionOrderService exceptionOrderService) {
    this.exceptionOrderMapper = exceptionOrderMapper;
    this.auditMapper = auditMapper;
    this.exceptionOrderService = exceptionOrderService;
  }

  /** 分页查询支付异常单，用于逾期支付等异常入账的运营处理。 */
  public ManagerPageResponse<PaymentExceptionOrder> pagePaymentExceptions(
      long page,
      long size,
      String merchantId,
      String cryptoOrderNo,
      String exceptionType,
      String status) {
    LambdaQueryWrapper<PaymentExceptionOrder> query =
        Wrappers.<PaymentExceptionOrder>lambdaQuery()
            .eq(hasText(cryptoOrderNo), PaymentExceptionOrder::getCryptoOrderNo, cryptoOrderNo)
            .eq(hasText(exceptionType), PaymentExceptionOrder::getExceptionType, normalizeFilterCode(exceptionType))
            .eq(hasText(status), PaymentExceptionOrder::getStatus, normalizeFilterCode(status));
    applyMerchantDataScope(query, PaymentExceptionOrder::getMerchantId, merchantId);
    query.orderByDesc(PaymentExceptionOrder::getCreatedAt);
    return page(exceptionOrderMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 查询支付异常单详情，并组合异常单审计记录。 */
  public ManagerPaymentExceptionDetailResponse paymentExceptionDetail(Long id) {
    PaymentExceptionOrder exceptionOrder =
        require(exceptionOrderMapper.selectById(id), "payment exception order not found");
    requireMerchantDataAccess(exceptionOrder.getMerchantId());
    return new ManagerPaymentExceptionDetailResponse(
        exceptionOrder,
        audits(auditMapper, exceptionOrder.getExceptionType(), exceptionOrder.getExceptionNo()));
  }

  /** 人工处理支付异常单；这里只更新异常单，不自动改变原订单状态。 */
  public PaymentExceptionOrder handlePaymentException(
      Long id, ManagerPaymentExceptionHandleRequest request) {
    PaymentExceptionOrder exceptionOrder =
        require(exceptionOrderMapper.selectById(id), "payment exception order not found");
    requireMerchantDataAccess(exceptionOrder.getMerchantId());
    PaymentExceptionStatus status = PaymentExceptionStatus.PROCESSING;
    if (request != null && hasText(request.status())) {
      status = PaymentExceptionStatus.valueOf(request.status().trim().toUpperCase());
    }
    return exceptionOrderService.handle(
        id,
        status,
        request == null ? null : request.operator(),
        request == null ? null : request.operatorNote());
  }
}
