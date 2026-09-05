package io.swzxsyh.payment.reconciliation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.callback.CallbackDeliveryStatus;
import io.swzxsyh.payment.domain.ContractSettlementStatus;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.ContractSettlementRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.mapper.PaymentOrderMapper;
import io.swzxsyh.payment.mapper.PaymentReconciliationRecordMapper;
import io.swzxsyh.payment.mapper.RawChainLogMapper;
import io.swzxsyh.payment.persistence.entity.ContractSettlementRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.persistence.entity.PaymentReconciliationRecord;
import io.swzxsyh.payment.persistence.entity.RawChainLog;
import io.swzxsyh.payment.rawchain.RawChainLogStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 支付对账服务，负责生成订单、链上流水、回调和合约分账之间的差异记录。 */
@Slf4j
@Service
public class PaymentReconciliationService {

  private final PaymentOrderMapper orderMapper;
  private final RawChainLogMapper rawChainLogMapper;
  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final ContractSettlementRecordMapper settlementMapper;
  private final PaymentReconciliationRecordMapper reconciliationMapper;

  public PaymentReconciliationService(
      PaymentOrderMapper orderMapper,
      RawChainLogMapper rawChainLogMapper,
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      ContractSettlementRecordMapper settlementMapper,
      PaymentReconciliationRecordMapper reconciliationMapper) {
    this.orderMapper = orderMapper;
    this.rawChainLogMapper = rawChainLogMapper;
    this.callbackMapper = callbackMapper;
    this.settlementMapper = settlementMapper;
    this.reconciliationMapper = reconciliationMapper;
  }

  /**
   * 按订单创建时间和链上流水观察时间生成一批对账结果。
   *
   * <p>该方法只写入/刷新 payment_reconciliation_record，不推进订单状态、不补发回调、不发起链上交易。
   */
  @Transactional
  public PaymentReconciliationSummary reconcile(LocalDateTime from, LocalDateTime to, int limit) {
    int safeLimit = Math.min(1000, Math.max(1, limit));
    LocalDateTime safeTo = to == null ? LocalDateTime.now() : to;
    LocalDateTime safeFrom = from == null ? safeTo.minusDays(1) : from;
    List<PaymentOrder> orders =
        orderMapper.selectList(
            Wrappers.<PaymentOrder>lambdaQuery()
                .ge(PaymentOrder::getCreatedAt, safeFrom)
                .lt(PaymentOrder::getCreatedAt, safeTo)
                .orderByAsc(PaymentOrder::getCreatedAt)
                .last("LIMIT " + safeLimit));
    List<RawChainLog> rawLogs =
        rawChainLogMapper.selectList(
            Wrappers.<RawChainLog>lambdaQuery()
                .eq(RawChainLog::getStatus, RawChainLogStatus.UNMATCHED.name())
                .ge(RawChainLog::getObservedAt, safeFrom)
                .lt(RawChainLog::getObservedAt, safeTo)
                .orderByAsc(RawChainLog::getObservedAt)
                .last("LIMIT " + safeLimit));

    Counter counter = new Counter();
    for (PaymentOrder order : orders) {
      upsert(buildOrderRecord(order, safeTo), counter);
      counter.orderChecked++;
    }
    for (RawChainLog rawLog : rawLogs) {
      upsert(buildRawLogRecord(rawLog, safeTo), counter);
      counter.rawLogChecked++;
    }
    log.info(
        "支付对账完成。from={}, to={}, orderChecked={}, rawLogChecked={}, matched={}, warning={}, mismatch={}",
        safeFrom,
        safeTo,
        counter.orderChecked,
        counter.rawLogChecked,
        counter.matched,
        counter.warning,
        counter.mismatch);
    return new PaymentReconciliationSummary(
        counter.orderChecked, counter.rawLogChecked, counter.matched, counter.warning, counter.mismatch);
  }

  /** 人工确认某条对账差异。 */
  @Transactional
  public PaymentReconciliationRecord manualConfirm(Long id, String operator, String note) {
    PaymentReconciliationRecord record = reconciliationMapper.selectById(id);
    if (record == null) {
      throw new IllegalArgumentException("reconciliation record not found");
    }
    record.setReconcileStatus(ReconciliationStatus.MANUAL_CONFIRMED.name());
    record.setOperator(operator);
    record.setOperatorNote(note);
    record.setManualConfirmedAt(LocalDateTime.now());
    record.setUpdatedAt(record.getManualConfirmedAt());
    reconciliationMapper.updateById(record);
    return record;
  }

  private PaymentReconciliationRecord buildOrderRecord(PaymentOrder order, LocalDateTime reconciledAt) {
    Optional<RawChainLog> rawLog = findRawLog(order);
    Optional<PaymentCallbackDeliveryRecord> callback = findLatestCallback(order.getCryptoOrderNo());
    Optional<ContractSettlementRecord> settlement = findSettlement(order.getCryptoOrderNo());

    PaymentReconciliationRecord record = baseRecord("ORDER", order.getCryptoOrderNo(), reconciledAt);
    record.setMerchantId(order.getMerchantId());
    record.setCryptoOrderNo(order.getCryptoOrderNo());
    record.setMerchantOrderNo(order.getMerchantOrderNo());
    record.setChain(order.getChain());
    record.setToken(order.getToken());
    record.setTokenAddress(order.getTokenAddress());
    record.setTxHash(order.getPaymentTxHash());
    record.setExpectedAmount(order.getAmount());
    record.setRealAmount(order.getRealAmount());
    record.setDiffAmount(diff(order.getRealAmount(), order.getAmount()));
    record.setOrderStatus(order.getStatus() == null ? null : order.getStatus().name());
    record.setRawLogStatus(rawLog.map(RawChainLog::getStatus).orElse(null));
    record.setCallbackStatus(callback.map(PaymentCallbackDeliveryRecord::getStatus).orElse(null));
    record.setSettlementStatus(settlement.map(ContractSettlementRecord::getStatus).orElse(null));

    applyOrderIssue(record, order, rawLog, callback, settlement);
    return record;
  }

  private PaymentReconciliationRecord buildRawLogRecord(RawChainLog rawLog, LocalDateTime reconciledAt) {
    String bizKey = rawLog.getChain() + ":" + rawLog.getTxHash() + ":" + normalizeLogIndex(rawLog.getLogIndex());
    PaymentReconciliationRecord record = baseRecord("RAW_CHAIN_LOG", bizKey, reconciledAt);
    record.setChain(rawLog.getChain());
    record.setToken(rawLog.getToken());
    record.setTokenAddress(rawLog.getTokenAddress());
    record.setTxHash(rawLog.getTxHash());
    record.setLogIndex(normalizeLogIndex(rawLog.getLogIndex()));
    record.setRealAmount(rawLog.getAmount());
    record.setRawLogStatus(rawLog.getStatus());
    record.setReconcileStatus(ReconciliationStatus.MISMATCH.name());
    record.setIssueType("UNMATCHED_CHAIN_LOG");
    record.setIssueReason("链上流水未匹配到支付订单，需要运营确认是否为错转、延迟支付或漏单");
    return record;
  }

  private PaymentReconciliationRecord baseRecord(String bizType, String bizKey, LocalDateTime reconciledAt) {
    PaymentReconciliationRecord record = new PaymentReconciliationRecord();
    record.setBizType(bizType);
    record.setBizKey(bizKey);
    record.setReconcileKey(bizType + ":" + bizKey);
    record.setReconciledAt(reconciledAt);
    return record;
  }

  private void applyOrderIssue(
      PaymentReconciliationRecord record,
      PaymentOrder order,
      Optional<RawChainLog> rawLog,
      Optional<PaymentCallbackDeliveryRecord> callback,
      Optional<ContractSettlementRecord> settlement) {
    if (order.getStatus() == OrderStatus.UNDERPAID || order.getStatus() == OrderStatus.OVERPAID) {
      record.setReconcileStatus(ReconciliationStatus.MISMATCH.name());
      record.setIssueType("AMOUNT_MISMATCH");
      record.setIssueReason("订单应付金额与链上实际到账金额不一致");
      return;
    }
    if (paidLike(order) && order.getRealAmount() != null && order.getAmount() != null
        && order.getRealAmount().compareTo(order.getAmount()) != 0) {
      record.setReconcileStatus(ReconciliationStatus.MISMATCH.name());
      record.setIssueType("AMOUNT_MISMATCH");
      record.setIssueReason("订单成功态金额与真实到账金额不一致");
      return;
    }
    if (StringUtils.hasText(order.getPaymentTxHash()) && rawLog.isEmpty()) {
      record.setReconcileStatus(ReconciliationStatus.WARNING.name());
      record.setIssueType("MISSING_RAW_CHAIN_LOG");
      record.setIssueReason("订单已有支付 txHash，但 raw_chain_logs 中未找到对应链上流水");
      return;
    }
    if (order.isLatePayment()) {
      record.setReconcileStatus(ReconciliationStatus.WARNING.name());
      record.setIssueType("LATE_PAYMENT");
      record.setIssueReason("订单存在超时后到账，需要异常单或人工处理闭环");
      return;
    }
    if (order.getStatus() == OrderStatus.KYT_REVIEW) {
      record.setReconcileStatus(ReconciliationStatus.WARNING.name());
      record.setIssueType("KYT_REVIEW");
      record.setIssueReason("订单处于 KYT 人工复核状态，暂不应回调成功");
      return;
    }
    if (paidLike(order) && callback.map(PaymentCallbackDeliveryRecord::getStatus)
        .filter(CallbackDeliveryStatus.DELIVERED.name()::equals)
        .isEmpty()) {
      record.setReconcileStatus(ReconciliationStatus.WARNING.name());
      record.setIssueType("CALLBACK_NOT_DELIVERED");
      record.setIssueReason("订单已入账，但未找到已成功投递的商户回调");
      return;
    }
    if (order.getPaymentMethod() == PaymentMethod.CONTRACT && settlement.isPresent()
        && !ContractSettlementStatus.RELEASED.name().equals(settlement.get().getStatus())) {
      record.setReconcileStatus(ReconciliationStatus.WARNING.name());
      record.setIssueType("SETTLEMENT_NOT_RELEASED");
      record.setIssueReason("合约支付已入账，但分账/放行记录未完成");
      return;
    }
    record.setReconcileStatus(ReconciliationStatus.MATCHED.name());
    record.setIssueType("NONE");
    record.setIssueReason("订单、链上流水、金额、回调和分账记录当前未发现差异");
  }

  private boolean paidLike(PaymentOrder order) {
    return order.getStatus() == OrderStatus.PAID
        || order.getStatus() == OrderStatus.UNDERPAID
        || order.getStatus() == OrderStatus.OVERPAID
        || order.getStatus() == OrderStatus.CONFIRMING;
  }

  private Optional<RawChainLog> findRawLog(PaymentOrder order) {
    if (!StringUtils.hasText(order.getPaymentTxHash())) {
      return Optional.empty();
    }
    return Optional.ofNullable(rawChainLogMapper.selectOne(
        Wrappers.<RawChainLog>lambdaQuery()
            .eq(StringUtils.hasText(order.getChain()), RawChainLog::getChain, order.getChain())
            .eq(RawChainLog::getTxHash, order.getPaymentTxHash())
            .last("LIMIT 1")));
  }

  private Optional<PaymentCallbackDeliveryRecord> findLatestCallback(String cryptoOrderNo) {
    if (!StringUtils.hasText(cryptoOrderNo)) {
      return Optional.empty();
    }
    return Optional.ofNullable(callbackMapper.selectOne(
        Wrappers.<PaymentCallbackDeliveryRecord>lambdaQuery()
            .eq(PaymentCallbackDeliveryRecord::getCryptoOrderNo, cryptoOrderNo)
            .orderByDesc(PaymentCallbackDeliveryRecord::getCreatedAt)
            .last("LIMIT 1")));
  }

  private Optional<ContractSettlementRecord> findSettlement(String cryptoOrderNo) {
    if (!StringUtils.hasText(cryptoOrderNo)) {
      return Optional.empty();
    }
    return Optional.ofNullable(settlementMapper.selectOne(
        Wrappers.<ContractSettlementRecord>lambdaQuery()
            .eq(ContractSettlementRecord::getCryptoOrderNo, cryptoOrderNo)
            .last("LIMIT 1")));
  }

  private BigDecimal diff(BigDecimal realAmount, BigDecimal expectedAmount) {
    if (realAmount == null || expectedAmount == null) {
      return null;
    }
    return realAmount.subtract(expectedAmount);
  }

  private Long normalizeLogIndex(Long logIndex) {
    return logIndex == null || logIndex < 0 ? 0L : logIndex;
  }

  private void upsert(PaymentReconciliationRecord record, Counter counter) {
    PaymentReconciliationRecord existing = reconciliationMapper.selectOne(
        Wrappers.<PaymentReconciliationRecord>lambdaQuery()
            .eq(PaymentReconciliationRecord::getReconcileKey, record.getReconcileKey())
            .last("LIMIT 1"));
    LocalDateTime now = LocalDateTime.now();
    if (existing != null && ReconciliationStatus.MANUAL_CONFIRMED.name().equals(existing.getReconcileStatus())) {
      return;
    }
    if (existing == null) {
      record.setCreatedAt(now);
    } else {
      record.setId(existing.getId());
      record.setCreatedAt(existing.getCreatedAt());
      record.setOperator(existing.getOperator());
      record.setOperatorNote(existing.getOperatorNote());
      record.setManualConfirmedAt(existing.getManualConfirmedAt());
    }
    record.setUpdatedAt(now);
    if (record.getId() == null) {
      reconciliationMapper.insert(record);
    } else {
      reconciliationMapper.updateById(record);
    }
    if (ReconciliationStatus.MATCHED.name().equals(record.getReconcileStatus())) {
      counter.matched++;
    } else if (ReconciliationStatus.WARNING.name().equals(record.getReconcileStatus())) {
      counter.warning++;
    } else if (ReconciliationStatus.MISMATCH.name().equals(record.getReconcileStatus())) {
      counter.mismatch++;
    }
  }

  private static final class Counter {
    private int orderChecked;
    private int rawLogChecked;
    private int matched;
    private int warning;
    private int mismatch;
  }
}
