package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerSettlementDtos.DetailResponse;
import io.swzxsyh.payment.mapper.ContractSettlementRecordMapper;
import io.swzxsyh.payment.mapper.ContractSettlementSplitRecordMapper;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.persistence.entity.ContractSettlementRecord;
import io.swzxsyh.payment.persistence.entity.ContractSettlementSplitRecord;
import io.swzxsyh.payment.settlement.record.ContractSettlementFinalizationService;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordService;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordView;
import org.springframework.stereotype.Service;

/** 管理端合约隔离、放行、分账记录查询和人工标记服务。 */
@Service
public class ManagerSettlementApplicationService extends ManagerApplicationSupport {

  private final ContractSettlementRecordMapper settlementMapper;
  private final ContractSettlementSplitRecordMapper settlementSplitMapper;
  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final ContractSettlementRecordService settlementRecordService;
  private final ContractSettlementFinalizationService settlementFinalizationService;

  public ManagerSettlementApplicationService(
      ContractSettlementRecordMapper settlementMapper,
      ContractSettlementSplitRecordMapper settlementSplitMapper,
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      PaymentAuditRecordMapper auditMapper,
      ContractSettlementRecordService settlementRecordService,
      ContractSettlementFinalizationService settlementFinalizationService) {
    this.settlementMapper = settlementMapper;
    this.settlementSplitMapper = settlementSplitMapper;
    this.callbackMapper = callbackMapper;
    this.auditMapper = auditMapper;
    this.settlementRecordService = settlementRecordService;
    this.settlementFinalizationService = settlementFinalizationService;
  }

  /** 分页查询合约结算记录，用于结算、清分和对账页面。 */
  public ManagerPageResponse<ContractSettlementRecord> pageSettlements(
      long page, long size, String chain, String token, String status, String cryptoOrderNo) {
    LambdaQueryWrapper<ContractSettlementRecord> query =
        Wrappers.<ContractSettlementRecord>lambdaQuery()
            .eq(hasText(chain), ContractSettlementRecord::getChain, normalizeFilterCode(chain))
            .eq(hasText(token), ContractSettlementRecord::getToken, normalizeFilterCode(token))
            .eq(hasText(status), ContractSettlementRecord::getStatus, normalizeFilterCode(status))
            .eq(hasText(cryptoOrderNo), ContractSettlementRecord::getCryptoOrderNo, cryptoOrderNo)
            .orderByDesc(ContractSettlementRecord::getCreatedAt);
    return page(settlementMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 查询合约结算详情，并组合回调和审计记录。 */
  public DetailResponse settlementDetail(String cryptoOrderNo) {
    ContractSettlementRecordView settlement =
        settlementRecordService.findByCryptoOrderNo(cryptoOrderNo)
            .orElseThrow(() -> new IllegalArgumentException("settlement not found"));
    return new DetailResponse(
        settlement,
        callbacksByBizOrderNo(callbackMapper, cryptoOrderNo),
        audits(auditMapper, "CRYPTO_ORDER", cryptoOrderNo));
  }

  /** 人工标记结算资金已进入隔离阶段。 */
  public ContractSettlementRecordView markSettlementIsolated(
      String cryptoOrderNo, String txHash, Long blockNumber) {
    return settlementRecordService.markIsolated(cryptoOrderNo, txHash, blockNumber)
        .orElseThrow(() -> new IllegalArgumentException("settlement not found"));
  }

  /** 人工标记隔离资金已放行并完成结算。 */
  public ContractSettlementRecordView markSettlementReleased(
      String cryptoOrderNo, String txHash, Long blockNumber) {
    return settlementFinalizationService.markReleasedAndFinalize(cryptoOrderNo, txHash, blockNumber);
  }

  /** 人工标记结算失败并记录失败原因。 */
  public ContractSettlementRecordView markSettlementFailed(
      String cryptoOrderNo, String txHash, String failureReason) {
    return settlementRecordService.markFailed(cryptoOrderNo, txHash, failureReason)
        .orElseThrow(() -> new IllegalArgumentException("settlement not found"));
  }

  /** 分页查询分账明细，用于查看收款人、比例和执行状态。 */
  public ManagerPageResponse<ContractSettlementSplitRecord> pageSettlementSplits(
      long page, long size, String cryptoOrderNo, String receiver, String status) {
    LambdaQueryWrapper<ContractSettlementSplitRecord> query =
        Wrappers.<ContractSettlementSplitRecord>lambdaQuery()
            .eq(hasText(cryptoOrderNo), ContractSettlementSplitRecord::getCryptoOrderNo, cryptoOrderNo)
            .eq(hasText(receiver), ContractSettlementSplitRecord::getReceiver, receiver)
            .eq(hasText(status), ContractSettlementSplitRecord::getStatus, normalizeFilterCode(status))
            .orderByDesc(ContractSettlementSplitRecord::getCreatedAt);
    return page(settlementSplitMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }
}
