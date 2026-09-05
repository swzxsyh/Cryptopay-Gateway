package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerCallbackDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.payment.callback.PaymentCallbackDeliveryService;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import org.springframework.stereotype.Service;

/** 管理端回调查询、详情和补发应用服务。 */
@Service
public class ManagerCallbackApplicationService extends ManagerApplicationSupport {

  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final PaymentCallbackDeliveryService callbackDeliveryService;

  public ManagerCallbackApplicationService(
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      PaymentAuditRecordMapper auditMapper,
      PaymentCallbackDeliveryService callbackDeliveryService) {
    this.callbackMapper = callbackMapper;
    this.auditMapper = auditMapper;
    this.callbackDeliveryService = callbackDeliveryService;
  }

  /** 分页查询回调投递记录，用于失败重试、死信和回执追踪页面。 */
  public ManagerPageResponse<PaymentCallbackDeliveryRecord> pageCallbacks(
      long page, long size, String merchantId, String bizOrderNo, String eventType, String status) {
    LambdaQueryWrapper<PaymentCallbackDeliveryRecord> query =
        Wrappers.<PaymentCallbackDeliveryRecord>lambdaQuery()
            .eq(hasText(bizOrderNo), PaymentCallbackDeliveryRecord::getCryptoOrderNo, bizOrderNo)
            .eq(hasText(eventType), PaymentCallbackDeliveryRecord::getEventType, eventType)
            .eq(hasText(status), PaymentCallbackDeliveryRecord::getStatus, normalizeFilterCode(status));
    applyMerchantDataScope(query, PaymentCallbackDeliveryRecord::getMerchantId, merchantId);
    query.orderByDesc(PaymentCallbackDeliveryRecord::getCreatedAt);
    return page(callbackMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 查询单条回调详情，并附带相关审计记录。 */
  public ManagerCallbackDetailResponse callbackDetail(Long id) {
    PaymentCallbackDeliveryRecord callback = require(callbackMapper.selectById(id), "callback not found");
    requireMerchantDataAccess(callback.getMerchantId());
    return new ManagerCallbackDetailResponse(
        callback, audits(auditMapper, callback.getEventType(), callback.getCryptoOrderNo()));
  }

  /** 手动触发指定回调记录补发。 */
  public void replayCallback(Long id) {
    PaymentCallbackDeliveryRecord callback = require(callbackMapper.selectById(id), "callback not found");
    requireMerchantDataAccess(callback.getMerchantId());
    callbackDeliveryService.replay(id);
  }
}
