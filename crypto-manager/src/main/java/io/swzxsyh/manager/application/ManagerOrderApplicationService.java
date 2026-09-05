package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerOrderDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerPaymentOrderView;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.mapper.PaymentOrderMapper;
import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordService;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordView;
import java.util.List;
import org.springframework.stereotype.Service;

/** 管理端普通支付订单查询与详情聚合服务。 */
@Service
public class ManagerOrderApplicationService extends ManagerApplicationSupport {

  private final PaymentOrderMapper orderMapper;
  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final ContractSettlementRecordService settlementRecordService;

  public ManagerOrderApplicationService(
      PaymentOrderMapper orderMapper,
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      PaymentAuditRecordMapper auditMapper,
      ContractSettlementRecordService settlementRecordService) {
    this.orderMapper = orderMapper;
    this.callbackMapper = callbackMapper;
    this.auditMapper = auditMapper;
    this.settlementRecordService = settlementRecordService;
  }

  /** 分页查询普通支付订单，支持商户、订单号、链、币种和状态筛选。 */
  public ManagerPageResponse<ManagerPaymentOrderView> pageOrders(
      long page,
      long size,
      String merchantId,
      String merchantOrderNo,
      String cryptoOrderNo,
      String chain,
      String token,
      String status) {
    LambdaQueryWrapper<PaymentOrder> query = Wrappers.<PaymentOrder>lambdaQuery()
        .eq(hasText(merchantOrderNo), PaymentOrder::getMerchantOrderNo, merchantOrderNo)
        .eq(hasText(cryptoOrderNo), PaymentOrder::getCryptoOrderNo, cryptoOrderNo)
        .eq(hasText(chain), PaymentOrder::getChain, normalizeFilterCode(chain))
        .eq(hasText(token), PaymentOrder::getToken, normalizeFilterCode(token))
        .eq(hasText(status), PaymentOrder::getStatus, normalizeFilterCode(status));
    applyMerchantDataScope(query, PaymentOrder::getMerchantId, merchantId);
    query.orderByDesc(PaymentOrder::getCreatedAt);
    Page<PaymentOrder> result = orderMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(ManagerPaymentOrderView::from).toList());
  }

  /** 查询普通支付订单详情，并组合回调、结算和审计记录。 */
  public ManagerOrderDetailResponse orderDetail(String cryptoOrderNo) {
    PaymentOrder order = require(orderMapper.selectById(cryptoOrderNo), "payment order not found");
    requireMerchantDataAccess(order.getMerchantId());
    List<PaymentCallbackDeliveryRecord> callbacks = callbacksByBizOrderNo(callbackMapper, cryptoOrderNo);
    ContractSettlementRecordView settlement =
        settlementRecordService.findByCryptoOrderNo(cryptoOrderNo).orElse(null);
    List<PaymentAuditRecord> audits = audits(auditMapper, "CRYPTO_ORDER", cryptoOrderNo);
    return new ManagerOrderDetailResponse(ManagerPaymentOrderView.from(order), callbacks, settlement, audits);
  }
}
