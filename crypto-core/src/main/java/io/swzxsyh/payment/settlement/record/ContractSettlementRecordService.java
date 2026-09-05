package io.swzxsyh.payment.settlement.record;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.payment.domain.ContractSettlementStatus;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.ContractSettlementRecordMapper;
import io.swzxsyh.payment.mapper.ContractSettlementSplitRecordMapper;
import io.swzxsyh.payment.persistence.entity.ContractSettlementRecord;
import io.swzxsyh.payment.persistence.entity.ContractSettlementSplitRecord;
import io.swzxsyh.payment.settlement.ContractSettlementPlan;
import io.swzxsyh.payment.settlement.SettlementSplit;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class ContractSettlementRecordService {

  private final ContractSettlementRecordMapper recordMapper;
  private final ContractSettlementSplitRecordMapper splitMapper;

  public ContractSettlementRecordService(
      ContractSettlementRecordMapper recordMapper,
      ContractSettlementSplitRecordMapper splitMapper) {
    this.recordMapper = recordMapper;
    this.splitMapper = splitMapper;
  }

  @Transactional
  public ContractSettlementRecordView recordPlanned(PaymentOrder order, ContractSettlementPlan plan) {
    ContractSettlementRecord record = findRecord(order.getCryptoOrderNo()).orElseGet(ContractSettlementRecord::new);
    if (record.getId() == null) {
      record.setCryptoOrderNo(order.getCryptoOrderNo());
      record.setCreatedAt(LocalDateTime.now());
    }
    record.setMerchantOrderNo(order.getMerchantOrderNo());
    record.setChain(plan.chain());
    record.setToken(plan.token());
    record.setTokenAddress(order.getTokenAddress());
    record.setContractAddress(plan.contractAddress());
    record.setAmount(plan.amount());
    record.setStatus(ContractSettlementStatus.PLANNED.name());
    record.setSettlementMode(order.getPaymentMethod() == null ? null : order.getPaymentMethod().name());
    record.setSplitCount(plan.splits().size());
    record.setTotalBasisPoints(plan.splits().stream().mapToInt(SettlementSplit::basisPoints).sum());
    record.setSignPayload(plan.signPayload());
    record.setSignature(plan.signature());
    record.setContractCallData(plan.contractCallData());
    record.setUpdatedAt(LocalDateTime.now());
    if (record.getPlannedAt() == null) {
      record.setPlannedAt(LocalDateTime.now());
    }

    if (record.getId() == null) {
      recordMapper.insert(record);
    } else {
      recordMapper.updateById(record);
      splitMapper.delete(new LambdaQueryWrapper<ContractSettlementSplitRecord>()
          .eq(ContractSettlementSplitRecord::getSettlementRecordId, record.getId()));
    }

    persistSplits(record, plan.splits());
    return toView(record);
  }

  @Transactional
  public Optional<ContractSettlementRecordView> markSubmitted(
      String cryptoOrderNo,
      String paymentTxHash,
      Long blockNumber) {
    return markIsolated(cryptoOrderNo, paymentTxHash, blockNumber);
  }

  /**
   * 标记资金已经进入隔离阶段。
   *
   * <p>这里记录的是“已确认进入隔离合约/隔离地址”的链上事实，
   * 但还没有把资金推进到最终放行状态。
   */
  @Transactional
  public Optional<ContractSettlementRecordView> markIsolated(
      String cryptoOrderNo,
      String paymentTxHash,
      Long blockNumber) {
    ContractSettlementRecord record = loadRecord(cryptoOrderNo);
    if (record == null) {
      return Optional.empty();
    }
    record.setPaymentTxHash(paymentTxHash);
    record.setBlockNumber(blockNumber);
    record.setStatus(ContractSettlementStatus.ISOLATED.name());
    record.setSubmittedAt(LocalDateTime.now());
    record.setUpdatedAt(LocalDateTime.now());
    recordMapper.updateById(record);
    return Optional.of(toView(record));
  }

  @Transactional
  public Optional<ContractSettlementRecordView> markSettled(
      String cryptoOrderNo,
      String paymentTxHash,
      Long blockNumber) {
    return markReleased(cryptoOrderNo, paymentTxHash, blockNumber);
  }

  /**
   * 标记隔离资金已经被放行并完成最终结算。
   */
  @Transactional
  public Optional<ContractSettlementRecordView> markReleased(
      String cryptoOrderNo,
      String paymentTxHash,
      Long blockNumber) {
    ContractSettlementRecord record = loadRecord(cryptoOrderNo);
    if (record == null) {
      return Optional.empty();
    }
    record.setPaymentTxHash(paymentTxHash);
    record.setBlockNumber(blockNumber);
    record.setStatus(ContractSettlementStatus.RELEASED.name());
    record.setSettledAt(LocalDateTime.now());
    record.setUpdatedAt(LocalDateTime.now());
    recordMapper.updateById(record);

    List<ContractSettlementSplitRecord> splits = splitMapper.selectList(
        new LambdaQueryWrapper<ContractSettlementSplitRecord>()
            .eq(ContractSettlementSplitRecord::getSettlementRecordId, record.getId()));
    LocalDateTime now = LocalDateTime.now();
    for (ContractSettlementSplitRecord split : splits) {
      split.setStatus(ContractSettlementStatus.RELEASED.name());
      split.setPaymentTxHash(paymentTxHash);
      split.setBlockNumber(blockNumber);
      split.setExecutedAt(now);
      split.setUpdatedAt(now);
      splitMapper.updateById(split);
    }
    return Optional.of(toView(record));
  }

  @Transactional
  public Optional<ContractSettlementRecordView> markFailed(
      String cryptoOrderNo,
      String paymentTxHash,
      String failureReason) {
    ContractSettlementRecord record = loadRecord(cryptoOrderNo);
    if (record == null) {
      return Optional.empty();
    }
    record.setPaymentTxHash(paymentTxHash);
    record.setStatus(ContractSettlementStatus.FAILED.name());
    record.setFailedAt(LocalDateTime.now());
    record.setFailureReason(failureReason);
    record.setUpdatedAt(LocalDateTime.now());
    recordMapper.updateById(record);
    return Optional.of(toView(record));
  }

  public Optional<ContractSettlementRecordView> findByCryptoOrderNo(String cryptoOrderNo) {
    ContractSettlementRecord record = loadRecord(cryptoOrderNo);
    return record == null ? Optional.empty() : Optional.of(toView(record));
  }

  public List<ContractSettlementRecordView> findAll(String chain, String status) {
    LambdaQueryWrapper<ContractSettlementRecord> query = new LambdaQueryWrapper<ContractSettlementRecord>()
        .eq(StringUtils.hasText(chain), ContractSettlementRecord::getChain, chain)
        .eq(StringUtils.hasText(status), ContractSettlementRecord::getStatus, status)
        .orderByDesc(ContractSettlementRecord::getCreatedAt);
    return recordMapper.selectList(query).stream()
        .map(this::toView)
        .toList();
  }

  public Optional<ContractSettlementRecordView> findByPaymentTxHash(String paymentTxHash) {
    if (!StringUtils.hasText(paymentTxHash)) {
      return Optional.empty();
    }
    ContractSettlementRecord record = recordMapper.selectOne(
        new LambdaQueryWrapper<ContractSettlementRecord>()
            .eq(ContractSettlementRecord::getPaymentTxHash, paymentTxHash));
    return record == null ? Optional.empty() : Optional.of(toView(record));
  }

  private void persistSplits(ContractSettlementRecord record, List<SettlementSplit> splits) {
    LocalDateTime now = LocalDateTime.now();
    for (SettlementSplit split : splits) {
      ContractSettlementSplitRecord entity = new ContractSettlementSplitRecord();
      entity.setSettlementRecordId(record.getId());
      entity.setCryptoOrderNo(record.getCryptoOrderNo());
      entity.setRole(split.role());
      entity.setReceiver(split.receiver());
      entity.setBasisPoints(split.basisPoints());
      entity.setAmount(split.amount());
      entity.setStatus(ContractSettlementStatus.PLANNED.name());
      entity.setCreatedAt(now);
      entity.setUpdatedAt(now);
      splitMapper.insert(entity);
    }
  }

  private ContractSettlementRecord loadRecord(String cryptoOrderNo) {
    if (!StringUtils.hasText(cryptoOrderNo)) {
      return null;
    }
    return findRecord(cryptoOrderNo).orElse(null);
  }

  private Optional<ContractSettlementRecord> findRecord(String cryptoOrderNo) {
    return Optional.ofNullable(recordMapper.selectOne(
        new LambdaQueryWrapper<ContractSettlementRecord>()
            .eq(ContractSettlementRecord::getCryptoOrderNo, cryptoOrderNo)));
  }

  private ContractSettlementRecordView toView(ContractSettlementRecord record) {
    List<ContractSettlementSplitView> splits = splitMapper.selectList(
            new LambdaQueryWrapper<ContractSettlementSplitRecord>()
                .eq(ContractSettlementSplitRecord::getSettlementRecordId, record.getId())
                .orderByAsc(ContractSettlementSplitRecord::getId))
        .stream()
        .map(split -> new ContractSettlementSplitView(
            split.getRole(),
            split.getReceiver(),
            split.getBasisPoints(),
            split.getAmount(),
            split.getStatus(),
            split.getPaymentTxHash(),
            split.getBlockNumber(),
            split.getExecutedAt()))
        .toList();
    return new ContractSettlementRecordView(
        record.getCryptoOrderNo(),
        record.getMerchantOrderNo(),
        record.getChain(),
        record.getToken(),
        record.getTokenAddress(),
        record.getContractAddress(),
        record.getAmount(),
        record.getStatus(),
        record.getSettlementMode(),
        record.getSplitCount(),
        record.getTotalBasisPoints(),
        record.getSignPayload(),
        record.getSignature(),
        record.getContractCallData(),
        record.getPaymentTxHash(),
        record.getBlockNumber(),
        record.getPlannedAt(),
        record.getSubmittedAt(),
        record.getSettledAt(),
        record.getFailedAt(),
        record.getFailureReason(),
        record.getCreatedAt(),
        record.getUpdatedAt(),
        splits
    );
  }
}
