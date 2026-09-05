package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAccountView;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustResponse;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.WithdrawHandleRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.WithdrawRecordView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.approval.ManagerApprovalRequired;
import io.swzxsyh.manager.security.ManagerSecurityContext;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.mapper.MerchantBalanceAdjustRecordMapper;
import io.swzxsyh.payment.mapper.MerchantBalanceAccountMapper;
import io.swzxsyh.payment.mapper.MerchantInfoMapper;
import io.swzxsyh.payment.mapper.MerchantWithdrawRecordMapper;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAdjustRecord;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAccount;
import io.swzxsyh.payment.persistence.entity.MerchantInfo;
import io.swzxsyh.payment.persistence.entity.MerchantWithdrawRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商户资金余额应用服务；manager 负责查看、调账和提现审核处理，提现发起留给未来商户端。 */
@Service
public class ManagerMerchantFundsApplicationService extends ManagerApplicationSupport {

  private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final MerchantBalanceAdjustRecordMapper adjustMapper;
  private final MerchantBalanceAccountMapper balanceMapper;
  private final MerchantWithdrawRecordMapper withdrawMapper;
  private final MerchantInfoMapper merchantInfoMapper;
  private final PaymentAuditService auditService;

  public ManagerMerchantFundsApplicationService(
      MerchantBalanceAdjustRecordMapper adjustMapper,
      MerchantBalanceAccountMapper balanceMapper,
      MerchantWithdrawRecordMapper withdrawMapper,
      MerchantInfoMapper merchantInfoMapper,
      PaymentAuditService auditService) {
    this.adjustMapper = adjustMapper;
    this.balanceMapper = balanceMapper;
    this.withdrawMapper = withdrawMapper;
    this.merchantInfoMapper = merchantInfoMapper;
    this.auditService = auditService;
  }

  /** 分页查询商户资金余额。 */
  public ManagerPageResponse<BalanceAccountView> pageBalances(
      long page, long size, String merchantId, String token) {
    LambdaQueryWrapper<MerchantBalanceAccount> query = Wrappers.<MerchantBalanceAccount>lambdaQuery()
        .eq(hasText(token), MerchantBalanceAccount::getBalanceToken, normalizeCode(token))
        .orderByAsc(MerchantBalanceAccount::getMerchantId)
        .orderByAsc(MerchantBalanceAccount::getBalanceToken);
    applyMerchantDataScope(query, MerchantBalanceAccount::getMerchantId, merchantId);
    Page<MerchantBalanceAccount> result = balanceMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(BalanceAccountView::from).toList());
  }

  /** 分页查询提现记录。 */
  public ManagerPageResponse<WithdrawRecordView> pageWithdrawals(
      long page, long size, String merchantId, String token, String status) {
    LambdaQueryWrapper<MerchantWithdrawRecord> query = Wrappers.<MerchantWithdrawRecord>lambdaQuery()
        .eq(hasText(token), MerchantWithdrawRecord::getToken, normalizeCode(token))
        .eq(hasText(status), MerchantWithdrawRecord::getStatus, normalizeCode(status))
        .orderByDesc(MerchantWithdrawRecord::getCreatedAt)
        .orderByDesc(MerchantWithdrawRecord::getId);
    applyMerchantDataScope(query, MerchantWithdrawRecord::getMerchantId, merchantId);
    Page<MerchantWithdrawRecord> result = withdrawMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(WithdrawRecordView::from).toList());
  }

  /** 人工调账；若配置了审批流会被审批切面拦截，审批完成前不会修改商户余额。 */
  @ManagerApprovalRequired(bizType = "MERCHANT_BALANCE_ADJUST")
  @Transactional(rollbackFor = Exception.class)
  public BalanceAdjustResponse adjustBalance(BalanceAdjustRequest request) {
    if (request == null || !hasText(request.merchantId()) || !hasText(request.token())
        || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("merchantId, token and positive amount are required");
    }
    String merchantId = request.merchantId().trim();
    requireMerchantDataAccess(merchantId);
    require(findMerchant(merchantId), "merchant not found");
    String token = normalizeCode(request.token());
    String direction = normalizeCode(request.direction());
    if (!"CREDIT".equals(direction) && !"DEBIT".equals(direction)) {
      throw new IllegalArgumentException("direction must be CREDIT or DEBIT");
    }
    MerchantBalanceAccount before = findAccount(merchantId, token);
    BigDecimal beforeAvailable = before == null ? BigDecimal.ZERO : before.getAvailableBalance();
    if ("CREDIT".equals(direction)) {
      balanceMapper.credit(merchantId, token, request.amount());
    } else if (balanceMapper.debitAvailable(merchantId, token, request.amount()) != 1) {
      throw new IllegalArgumentException("insufficient merchant available balance");
    }
    MerchantBalanceAccount after = require(findAccount(merchantId, token), "merchant balance account not found");
    MerchantBalanceAdjustRecord record = new MerchantBalanceAdjustRecord();
    record.setAdjustNo(newAdjustNo());
    record.setMerchantId(merchantId);
    record.setToken(token);
    record.setDirection(direction);
    record.setAmount(request.amount());
    record.setReason(trimToNull(request.reason()));
    record.setOperator(currentUsername());
    record.setOperatorNote(trimToNull(request.operatorNote()));
    record.setBeforeAvailableBalance(beforeAvailable);
    record.setAfterAvailableBalance(after.getAvailableBalance());
    record.setCreatedAt(LocalDateTime.now());
    adjustMapper.insert(record);
    auditService.record("MERCHANT_BALANCE_ADJUSTED", "MERCHANT_BALANCE_ADJUST", record.getAdjustNo(), direction,
        Map.of(
            "merchantId", merchantId,
            "adjustNo", record.getAdjustNo(),
            "token", token,
            "direction", direction,
            "amount", request.amount(),
            "beforeAvailableBalance", beforeAvailable,
            "afterAvailableBalance", after.getAvailableBalance()));
    return new BalanceAdjustResponse(
        null, record.getAdjustNo(), merchantId, token, direction, request.amount(), "EXECUTED");
  }

  /** 审核通过商户端提交的提现单，并冻结对应余额。 */
  @Transactional(rollbackFor = Exception.class)
  public WithdrawRecordView approveWithdraw(Long id, WithdrawHandleRequest request) {
    MerchantWithdrawRecord record = requireRecord(id);
    requireStatus(record, "SUBMITTED");
    if (balanceMapper.freezeForWithdraw(record.getMerchantId(), record.getToken(), record.getAmount()) != 1) {
      throw new IllegalArgumentException("insufficient merchant available balance");
    }
    LocalDateTime now = LocalDateTime.now();
    record.setStatus("APPROVED");
    record.setOperator(currentUsername());
    record.setOperatorNote(request == null ? null : trimToNull(request.operatorNote()));
    record.setReviewedAt(now);
    record.setUpdatedAt(now);
    withdrawMapper.updateById(record);
    auditWithdraw(record, "MERCHANT_WITHDRAW_APPROVED");
    return WithdrawRecordView.from(record);
  }

  /** 审核拒绝商户端提交的提现单；未冻结资金，因此不做余额释放。 */
  @Transactional(rollbackFor = Exception.class)
  public WithdrawRecordView rejectWithdraw(Long id, WithdrawHandleRequest request) {
    MerchantWithdrawRecord record = requireRecord(id);
    requireStatus(record, "SUBMITTED");
    LocalDateTime now = LocalDateTime.now();
    record.setStatus("REJECTED");
    record.setOperator(currentUsername());
    record.setOperatorNote(request == null ? null : trimToNull(request.operatorNote()));
    record.setFailureReason(request == null ? null : trimToNull(request.failureReason()));
    record.setReviewedAt(now);
    record.setCompletedAt(now);
    record.setUpdatedAt(now);
    withdrawMapper.updateById(record);
    auditWithdraw(record, "MERCHANT_WITHDRAW_REJECTED");
    return WithdrawRecordView.from(record);
  }

  /** 标记提现进入链上处理中，通常用于运营已发起链上付款后登记 txHash。 */
  @Transactional(rollbackFor = Exception.class)
  public WithdrawRecordView markWithdrawProcessing(Long id, WithdrawHandleRequest request) {
    MerchantWithdrawRecord record = requireRecord(id);
    requireStatus(record, "APPROVED");
    LocalDateTime now = LocalDateTime.now();
    record.setStatus("PROCESSING");
    record.setTxHash(request == null ? null : trimToNull(request.txHash()));
    record.setOperator(currentUsername());
    record.setOperatorNote(request == null ? null : trimToNull(request.operatorNote()));
    record.setSubmittedAt(now);
    record.setUpdatedAt(now);
    withdrawMapper.updateById(record);
    auditWithdraw(record, "MERCHANT_WITHDRAW_PROCESSING");
    return WithdrawRecordView.from(record);
  }

  /** 标记提现成功，并从冻结余额转入累计提现。 */
  @Transactional(rollbackFor = Exception.class)
  public WithdrawRecordView markWithdrawSucceeded(Long id, WithdrawHandleRequest request) {
    MerchantWithdrawRecord record = requireRecord(id);
    requireStatus(record, "PROCESSING");
    if (balanceMapper.completeWithdraw(record.getMerchantId(), record.getToken(), record.getAmount()) != 1) {
      throw new IllegalArgumentException("merchant frozen balance is insufficient");
    }
    LocalDateTime now = LocalDateTime.now();
    record.setStatus("SUCCEEDED");
    if (hasText(request == null ? null : request.txHash())) {
      record.setTxHash(request.txHash().trim());
    }
    record.setOperator(currentUsername());
    record.setOperatorNote(request == null ? null : trimToNull(request.operatorNote()));
    record.setCompletedAt(now);
    record.setUpdatedAt(now);
    withdrawMapper.updateById(record);
    auditWithdraw(record, "MERCHANT_WITHDRAW_SUCCEEDED");
    return WithdrawRecordView.from(record);
  }

  /** 标记提现失败，并把冻结余额释放回商户可用余额。 */
  @Transactional(rollbackFor = Exception.class)
  public WithdrawRecordView markWithdrawFailed(Long id, WithdrawHandleRequest request) {
    MerchantWithdrawRecord record = requireRecord(id);
    requireStatus(record, "PROCESSING");
    if (balanceMapper.releaseWithdraw(record.getMerchantId(), record.getToken(), record.getAmount()) != 1) {
      throw new IllegalArgumentException("merchant frozen balance is insufficient");
    }
    LocalDateTime now = LocalDateTime.now();
    record.setStatus("FAILED");
    record.setOperator(currentUsername());
    record.setOperatorNote(request == null ? null : trimToNull(request.operatorNote()));
    record.setFailureReason(request == null ? null : trimToNull(request.failureReason()));
    record.setCompletedAt(now);
    record.setUpdatedAt(now);
    withdrawMapper.updateById(record);
    auditWithdraw(record, "MERCHANT_WITHDRAW_FAILED");
    return WithdrawRecordView.from(record);
  }

  private MerchantWithdrawRecord requireRecord(Long id) {
    MerchantWithdrawRecord record = require(withdrawMapper.selectById(id), "withdraw record not found");
    requireMerchantDataAccess(record.getMerchantId());
    return record;
  }

  private void requireStatus(MerchantWithdrawRecord record, String status) {
    if (!status.equals(record.getStatus())) {
      throw new IllegalArgumentException("withdraw status must be " + status);
    }
  }

  private void auditWithdraw(MerchantWithdrawRecord record, String eventType) {
    auditService.record(eventType, "MERCHANT_WITHDRAW", record.getWithdrawNo(), record.getStatus(),
        Map.of(
            "withdrawNo", record.getWithdrawNo(),
            "merchantId", record.getMerchantId(),
            "token", record.getToken(),
            "amount", record.getAmount(),
            "status", record.getStatus()));
  }

  private MerchantInfo findMerchant(String merchantId) {
    return merchantInfoMapper.selectOne(Wrappers.<MerchantInfo>lambdaQuery()
        .eq(MerchantInfo::getMerchantId, merchantId)
        .last("limit 1"));
  }

  private MerchantBalanceAccount findAccount(String merchantId, String token) {
    return balanceMapper.selectOne(Wrappers.<MerchantBalanceAccount>lambdaQuery()
        .eq(MerchantBalanceAccount::getMerchantId, merchantId)
        .eq(MerchantBalanceAccount::getBalanceToken, token)
        .last("limit 1"));
  }

  private String newAdjustNo() {
    return "MA" + LocalDateTime.now().format(NO_TIME)
        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }

  private String normalizeCode(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    return hasText(value) ? value.trim() : null;
  }

  private String currentUsername() {
    return ManagerSecurityContext.requireCurrent().getUsername();
  }
}
