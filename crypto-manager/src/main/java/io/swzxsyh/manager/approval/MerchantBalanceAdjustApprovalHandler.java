package io.swzxsyh.manager.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustRequest;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.mapper.MerchantBalanceAdjustRecordMapper;
import io.swzxsyh.payment.mapper.MerchantBalanceAccountMapper;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAdjustRecord;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAccount;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 商户余额调账审批执行器；只有审批流最终通过后才真正改余额。 */
@Component
public class MerchantBalanceAdjustApprovalHandler implements ApprovalBusinessHandler {

  private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final MerchantBalanceAccountMapper balanceMapper;
  private final MerchantBalanceAdjustRecordMapper adjustMapper;
  private final PaymentAuditService auditService;
  private final ObjectMapper objectMapper;

  public MerchantBalanceAdjustApprovalHandler(
      MerchantBalanceAccountMapper balanceMapper,
      MerchantBalanceAdjustRecordMapper adjustMapper,
      PaymentAuditService auditService,
      ObjectMapper objectMapper) {
    this.balanceMapper = balanceMapper;
    this.adjustMapper = adjustMapper;
    this.auditService = auditService;
    this.objectMapper = objectMapper;
  }

  @Override
  public String bizType() {
    return "MERCHANT_BALANCE_ADJUST";
  }

  @Override
  public void executeApproved(ManagerApprovalRequest request) {
    BalanceAdjustRequest payload = readPayload(request.getPayloadJson());
    String merchantId = payload.merchantId().trim();
    String token = payload.token().trim().toUpperCase();
    String direction = payload.direction().trim().toUpperCase();
    BigDecimal amount = payload.amount();
    MerchantBalanceAccount before = findAccount(merchantId, token);
    BigDecimal beforeAvailable = before == null ? BigDecimal.ZERO : before.getAvailableBalance();
    if ("CREDIT".equals(direction)) {
      balanceMapper.credit(merchantId, token, amount);
    } else if (balanceMapper.debitAvailable(merchantId, token, amount) != 1) {
      throw new IllegalArgumentException("insufficient merchant available balance");
    }
    MerchantBalanceAccount after = findAccount(merchantId, token);
    BigDecimal afterAvailable = after == null ? BigDecimal.ZERO : after.getAvailableBalance();
    MerchantBalanceAdjustRecord record = new MerchantBalanceAdjustRecord();
    record.setAdjustNo(newAdjustNo());
    record.setMerchantId(merchantId);
    record.setToken(token);
    record.setDirection(direction);
    record.setAmount(amount);
    record.setReason(trimToNull(payload.reason()));
    record.setOperator("approval");
    record.setOperatorNote("approvalNo=" + request.getApprovalNo()
        + (StringUtils.hasText(payload.operatorNote()) ? "; " + payload.operatorNote().trim() : ""));
    record.setBeforeAvailableBalance(beforeAvailable);
    record.setAfterAvailableBalance(afterAvailable);
    record.setCreatedAt(LocalDateTime.now());
    adjustMapper.insert(record);
    auditService.record("MERCHANT_BALANCE_ADJUSTED", "MERCHANT_BALANCE_ADJUST", record.getAdjustNo(), direction,
        Map.of(
            "approvalNo", request.getApprovalNo(),
            "merchantId", merchantId,
            "adjustNo", record.getAdjustNo(),
            "token", token,
            "direction", direction,
            "amount", amount,
            "beforeAvailableBalance", beforeAvailable,
            "afterAvailableBalance", afterAvailable));
  }

  private BalanceAdjustRequest readPayload(String payloadJson) {
    try {
      return objectMapper.readValue(payloadJson, BalanceAdjustRequest.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("failed to deserialize balance adjust approval payload", e);
    }
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

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
