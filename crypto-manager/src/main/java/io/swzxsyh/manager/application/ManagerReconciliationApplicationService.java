package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerReconciliationDtos.HandleRequest;
import io.swzxsyh.manager.api.dto.ManagerReconciliationDtos.RunRequest;
import io.swzxsyh.payment.mapper.PaymentReconciliationRecordMapper;
import io.swzxsyh.payment.persistence.entity.PaymentReconciliationRecord;
import io.swzxsyh.payment.reconciliation.PaymentReconciliationService;
import io.swzxsyh.payment.reconciliation.PaymentReconciliationSummary;
import org.springframework.stereotype.Service;

/** 管理端支付对账应用服务。 */
@Service
public class ManagerReconciliationApplicationService extends ManagerApplicationSupport {

  private final PaymentReconciliationService reconciliationService;
  private final PaymentReconciliationRecordMapper reconciliationMapper;

  public ManagerReconciliationApplicationService(
      PaymentReconciliationService reconciliationService,
      PaymentReconciliationRecordMapper reconciliationMapper) {
    this.reconciliationService = reconciliationService;
    this.reconciliationMapper = reconciliationMapper;
  }

  /** 手动触发一批订单和链上流水对账。 */
  public PaymentReconciliationSummary run(RunRequest request) {
    int limit = request == null || request.limit() == null ? 500 : request.limit();
    return reconciliationService.reconcile(
        request == null ? null : request.from(),
        request == null ? null : request.to(),
        limit);
  }

  /** 分页查询对账结果。 */
  public ManagerPageResponse<PaymentReconciliationRecord> page(
      long page,
      long size,
      String merchantId,
      String cryptoOrderNo,
      String chain,
      String token,
      String reconcileStatus,
      String issueType,
      String txHash) {
    LambdaQueryWrapper<PaymentReconciliationRecord> query =
        Wrappers.<PaymentReconciliationRecord>lambdaQuery()
            .eq(hasText(cryptoOrderNo), PaymentReconciliationRecord::getCryptoOrderNo, cryptoOrderNo)
            .eq(hasText(chain), PaymentReconciliationRecord::getChain, normalizeFilterCode(chain))
            .eq(hasText(token), PaymentReconciliationRecord::getToken, normalizeFilterCode(token))
            .eq(hasText(reconcileStatus), PaymentReconciliationRecord::getReconcileStatus, normalizeFilterCode(reconcileStatus))
            .eq(hasText(issueType), PaymentReconciliationRecord::getIssueType, normalizeFilterCode(issueType))
            .eq(hasText(txHash), PaymentReconciliationRecord::getTxHash, txHash);
    applyMerchantDataScope(query, PaymentReconciliationRecord::getMerchantId, merchantId);
    query.orderByDesc(PaymentReconciliationRecord::getReconciledAt)
        .orderByDesc(PaymentReconciliationRecord::getId);
    return page(reconciliationMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 人工确认对账结果，确认后自动对账不会覆盖为其它状态。 */
  public PaymentReconciliationRecord manualConfirm(Long id, HandleRequest request) {
    PaymentReconciliationRecord record =
        require(reconciliationMapper.selectById(id), "reconciliation record not found");
    requireMerchantDataAccess(record.getMerchantId());
    return reconciliationService.manualConfirm(
        id,
        request == null ? null : request.operator(),
        request == null ? null : request.operatorNote());
  }
}
