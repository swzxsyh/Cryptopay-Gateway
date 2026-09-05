package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import org.springframework.stereotype.Service;

/** 管理端审计记录查询服务。 */
@Service
public class ManagerAuditApplicationService extends ManagerApplicationSupport {

  private final PaymentAuditRecordMapper auditMapper;

  public ManagerAuditApplicationService(PaymentAuditRecordMapper auditMapper) {
    this.auditMapper = auditMapper;
  }

  /** 分页查询审计记录，支持事件、业务类型、业务键和商户筛选。 */
  public ManagerPageResponse<PaymentAuditRecord> pageAudits(
      long page, long size, String eventType, String bizType, String bizKey, String merchantId) {
    LambdaQueryWrapper<PaymentAuditRecord> query = Wrappers.<PaymentAuditRecord>lambdaQuery()
        .eq(hasText(eventType), PaymentAuditRecord::getEventType, eventType)
        .eq(hasText(bizType), PaymentAuditRecord::getBizType, bizType)
        .eq(hasText(bizKey), PaymentAuditRecord::getBizKey, bizKey);
    applyMerchantDataScope(query, PaymentAuditRecord::getMerchantId, merchantId);
    query.orderByDesc(PaymentAuditRecord::getCreatedAt);
    return page(auditMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }
}
