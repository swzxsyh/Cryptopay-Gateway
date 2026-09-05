package io.swzxsyh.payment.exceptionorder;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.kyt.KytDecision;
import io.swzxsyh.payment.kyt.KytScreeningResult;
import io.swzxsyh.payment.mapper.PaymentExceptionOrderMapper;
import io.swzxsyh.payment.persistence.entity.PaymentExceptionOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 支付异常单服务，负责把异常链上事实沉淀给运营人工处理。 */
@Slf4j
@Service
public class PaymentExceptionOrderService {

  private static final DateTimeFormatter EXCEPTION_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final PaymentExceptionOrderMapper exceptionOrderMapper;
  private final PaymentAuditService auditService;

  public PaymentExceptionOrderService(
      PaymentExceptionOrderMapper exceptionOrderMapper, PaymentAuditService auditService) {
    this.exceptionOrderMapper = exceptionOrderMapper;
    this.auditService = auditService;
  }

  /** 为逾期支付创建异常单；重复交易重复扫描时返回已有异常单。 */
  @Transactional
  public PaymentExceptionOrder createLatePaymentException(
      PaymentOrder order,
      String chain,
      String sourceAddress,
      String destinationAddress,
      String txHash,
      BigDecimal realAmount,
      String tokenAddress,
      Long blockNumber) {
    if (order == null || !StringUtils.hasText(order.getCryptoOrderNo()) || !StringUtils.hasText(txHash)) {
      throw new IllegalArgumentException("order and txHash are required for payment exception");
    }
    Optional<PaymentExceptionOrder> existing = findByOrderAndTxHash(order.getCryptoOrderNo(), txHash);
    if (existing.isPresent()) {
      return existing.get();
    }

    LocalDateTime now = LocalDateTime.now();
    PaymentExceptionOrder exceptionOrder = new PaymentExceptionOrder();
    exceptionOrder.setExceptionNo(generateExceptionNo(now));
    exceptionOrder.setExceptionType(PaymentExceptionType.LATE_PAYMENT.name());
    exceptionOrder.setStatus(PaymentExceptionStatus.PENDING.name());
    exceptionOrder.setMerchantId(order.getMerchantId());
    exceptionOrder.setMerchantOrderNo(order.getMerchantOrderNo());
    exceptionOrder.setCryptoOrderNo(order.getCryptoOrderNo());
    exceptionOrder.setChain(StringUtils.hasText(chain) ? chain : order.getChain());
    exceptionOrder.setToken(order.getToken());
    exceptionOrder.setTokenAddress(StringUtils.hasText(tokenAddress) ? tokenAddress : order.getTokenAddress());
    exceptionOrder.setPaymentAddress(StringUtils.hasText(destinationAddress) ? destinationAddress : order.getPaymentAddress());
    exceptionOrder.setSourceAddress(sourceAddress);
    exceptionOrder.setTxHash(txHash);
    exceptionOrder.setExpectedAmount(order.getAmount());
    exceptionOrder.setRealAmount(realAmount);
    exceptionOrder.setBlockNumber(blockNumber);
    exceptionOrder.setOrderStatus(order.getStatus() == null ? null : order.getStatus().name());
    exceptionOrder.setReason("订单过期后监听到链上入账，原订单保持过期状态，等待运营人工确认处理。");
    exceptionOrder.setCreatedAt(now);
    exceptionOrder.setUpdatedAt(now);
    exceptionOrderMapper.insert(exceptionOrder);
    auditService.record(
        "PAYMENT_EXCEPTION_CREATED",
        PaymentExceptionType.LATE_PAYMENT.name(),
        exceptionOrder.getExceptionNo(),
        exceptionOrder.getStatus(),
        exceptionOrder);
    log.warn(
        "逾期支付异常单已创建。exceptionNo={}, cryptoOrderNo={}, merchantId={}, txHash={}, expectedAmount={}, realAmount={}, status={}",
        exceptionOrder.getExceptionNo(),
        order.getCryptoOrderNo(),
        order.getMerchantId(),
        txHash,
        order.getAmount(),
        realAmount,
        exceptionOrder.getStatus());
    return exceptionOrder;
  }

  /** 为 KYT 风险入账创建或更新异常单；若同一交易已有逾期异常单，则补充 KYT 风险信息。 */
  @Transactional
  public PaymentExceptionOrder createKytPaymentException(
      PaymentOrder order,
      String chain,
      String sourceAddress,
      String destinationAddress,
      String txHash,
      BigDecimal realAmount,
      String tokenAddress,
      Long blockNumber,
      KytScreeningResult kytResult) {
    if (order == null || !StringUtils.hasText(order.getCryptoOrderNo()) || !StringUtils.hasText(txHash)) {
      throw new IllegalArgumentException("order and txHash are required for KYT payment exception");
    }
    KytDecision decision = kytResult == null ? KytDecision.REVIEW : kytResult.decision();
    String riskSummary = buildKytRiskReason(kytResult);
    Optional<PaymentExceptionOrder> existing = findByOrderAndTxHash(order.getCryptoOrderNo(), txHash);
    if (existing.isPresent()) {
      PaymentExceptionOrder exceptionOrder = existing.get();
      exceptionOrder.setReason(mergeReason(exceptionOrder.getReason(), riskSummary));
      exceptionOrder.setStatus(PaymentExceptionStatus.PENDING.name());
      exceptionOrder.setUpdatedAt(LocalDateTime.now());
      exceptionOrderMapper.updateById(exceptionOrder);
      auditService.record(
          "PAYMENT_EXCEPTION_KYT_UPDATED",
          exceptionOrder.getExceptionType(),
          exceptionOrder.getExceptionNo(),
          exceptionOrder.getStatus(),
          exceptionOrder);
      return exceptionOrder;
    }

    LocalDateTime now = LocalDateTime.now();
    PaymentExceptionOrder exceptionOrder = new PaymentExceptionOrder();
    exceptionOrder.setExceptionNo(generateExceptionNo(now));
    exceptionOrder.setExceptionType(
        decision == KytDecision.REJECT
            ? PaymentExceptionType.KYT_REJECTED.name()
            : PaymentExceptionType.KYT_REVIEW.name());
    exceptionOrder.setStatus(PaymentExceptionStatus.PENDING.name());
    exceptionOrder.setMerchantId(order.getMerchantId());
    exceptionOrder.setMerchantOrderNo(order.getMerchantOrderNo());
    exceptionOrder.setCryptoOrderNo(order.getCryptoOrderNo());
    exceptionOrder.setChain(StringUtils.hasText(chain) ? chain : order.getChain());
    exceptionOrder.setToken(order.getToken());
    exceptionOrder.setTokenAddress(StringUtils.hasText(tokenAddress) ? tokenAddress : order.getTokenAddress());
    exceptionOrder.setPaymentAddress(StringUtils.hasText(destinationAddress) ? destinationAddress : order.getPaymentAddress());
    exceptionOrder.setSourceAddress(sourceAddress);
    exceptionOrder.setTxHash(txHash);
    exceptionOrder.setExpectedAmount(order.getAmount());
    exceptionOrder.setRealAmount(realAmount);
    exceptionOrder.setBlockNumber(blockNumber);
    exceptionOrder.setOrderStatus(order.getStatus() == null ? null : order.getStatus().name());
    exceptionOrder.setReason(riskSummary);
    exceptionOrder.setCreatedAt(now);
    exceptionOrder.setUpdatedAt(now);
    exceptionOrderMapper.insert(exceptionOrder);
    auditService.record(
        "PAYMENT_EXCEPTION_CREATED",
        exceptionOrder.getExceptionType(),
        exceptionOrder.getExceptionNo(),
        exceptionOrder.getStatus(),
        exceptionOrder);
    log.warn(
        "KYT 风险异常单已创建。exceptionNo={}, cryptoOrderNo={}, merchantId={}, txHash={}, decision={}, realAmount={}",
        exceptionOrder.getExceptionNo(),
        order.getCryptoOrderNo(),
        order.getMerchantId(),
        txHash,
        decision,
        realAmount);
    return exceptionOrder;
  }

  /** 查询指定订单和交易哈希是否已存在异常单。 */
  public Optional<PaymentExceptionOrder> findByOrderAndTxHash(String cryptoOrderNo, String txHash) {
    if (!StringUtils.hasText(cryptoOrderNo) || !StringUtils.hasText(txHash)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        exceptionOrderMapper.selectOne(
            Wrappers.<PaymentExceptionOrder>lambdaQuery()
                .eq(PaymentExceptionOrder::getCryptoOrderNo, cryptoOrderNo)
                .eq(PaymentExceptionOrder::getTxHash, txHash)
                .last("LIMIT 1")));
  }

  /** 人工处理异常单。 */
  @Transactional
  public PaymentExceptionOrder handle(
      Long id, PaymentExceptionStatus status, String operator, String operatorNote) {
    PaymentExceptionOrder exceptionOrder = exceptionOrderMapper.selectById(id);
    if (exceptionOrder == null) {
      throw new IllegalArgumentException("payment exception order not found");
    }
    exceptionOrder.setStatus(status == null ? PaymentExceptionStatus.PROCESSING.name() : status.name());
    exceptionOrder.setOperator(operator);
    exceptionOrder.setOperatorNote(operatorNote);
    exceptionOrder.setHandledAt(LocalDateTime.now());
    exceptionOrder.setUpdatedAt(LocalDateTime.now());
    exceptionOrderMapper.updateById(exceptionOrder);
    auditService.record(
        "PAYMENT_EXCEPTION_HANDLED",
        exceptionOrder.getExceptionType(),
        exceptionOrder.getExceptionNo(),
        exceptionOrder.getStatus(),
        exceptionOrder);
    return exceptionOrder;
  }

  private String generateExceptionNo(LocalDateTime now) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    return "PE" + EXCEPTION_TIME_FORMAT.format(now) + suffix;
  }

  private String buildKytRiskReason(KytScreeningResult result) {
    if (result == null) {
      return "KYT 风控结果缺失，进入人工复核。";
    }
    String reasons = result.reasons() == null || result.reasons().isEmpty()
        ? "无明细原因"
        : String.join("; ", result.reasons());
    return "KYT 风控命中，decision=" + result.decision()
        + ", provider=" + result.selectedProvider()
        + ", riskScore=" + result.riskScore()
        + ", reasons=" + reasons;
  }

  private String mergeReason(String existingReason, String appendReason) {
    if (!StringUtils.hasText(existingReason)) {
      return appendReason;
    }
    if (!StringUtils.hasText(appendReason) || existingReason.contains(appendReason)) {
      return existingReason;
    }
    return existingReason + "；" + appendReason;
  }
}
