package io.swzxsyh.payment.accounting;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.MerchantBalanceAccountMapper;
import io.swzxsyh.payment.mapper.MerchantBalanceFlowRecordMapper;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAccount;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceFlowRecord;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 商户余额入账服务，统一处理普通订单、订阅账单的净额入账和资金流水。 */
@Slf4j
@Service
public class MerchantBalanceLedgerService {

  private static final DateTimeFormatter FLOW_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final MerchantBalanceAccountMapper balanceMapper;
  private final MerchantBalanceFlowRecordMapper flowMapper;
  private final MerchantSettlementFeeCalculator feeCalculator;
  private final CryptoPaymentProperties properties;
  private final LockUtil lockUtil;

  public MerchantBalanceLedgerService(
      MerchantBalanceAccountMapper balanceMapper,
      MerchantBalanceFlowRecordMapper flowMapper,
      MerchantSettlementFeeCalculator feeCalculator,
      CryptoPaymentProperties properties,
      LockUtil lockUtil) {
    this.balanceMapper = balanceMapper;
    this.flowMapper = flowMapper;
    this.feeCalculator = feeCalculator;
    this.properties = properties;
    this.lockUtil = lockUtil;
  }

  /** 普通支付订单成功后按订单费率快照给商户余额入账。 */
  @Transactional(rollbackFor = Exception.class)
  public Optional<MerchantBalanceFlowRecord> creditPaymentOrder(PaymentOrder order) {
    if (order == null || !StringUtils.hasText(order.getCryptoOrderNo())) {
      return Optional.empty();
    }
    return creditWithLock(
        "PAYMENT_ORDER",
        order.getCryptoOrderNo(),
        () -> creditPaymentOrderLocked(order));
  }

  /** 订阅周期账单成功后按账单费率快照给商户余额入账。 */
  @Transactional(rollbackFor = Exception.class)
  public Optional<MerchantBalanceFlowRecord> creditSubscriptionBilling(
      SubscriptionOrder order, SubscriptionBillingRecord bill) {
    if (order == null || bill == null || bill.getId() == null) {
      return Optional.empty();
    }
    return creditWithLock(
        "SUBSCRIPTION_BILLING",
        String.valueOf(bill.getId()),
        () -> creditSubscriptionBillingLocked(order, bill));
  }

  private Optional<MerchantBalanceFlowRecord> creditWithLock(
      String bizType, String bizNo, LedgerAction action) {
    String lockKey = RedisKeyNamespace.merchantBalancePostingLock(properties, bizType, bizNo);
    return lockUtil.withLock(lockKey, 2000, 60, action::execute);
  }

  protected Optional<MerchantBalanceFlowRecord> creditPaymentOrderLocked(PaymentOrder order) {
    String bizType = "PAYMENT_ORDER";
    String bizNo = order.getCryptoOrderNo();
    if (findExisting(bizType, bizNo).isPresent()) {
      log.info("普通订单商户余额已入账，跳过重复处理。cryptoOrderNo={}", bizNo);
      return Optional.empty();
    }

    BigDecimal grossAmount = value(order.getRealAmount(), order.getAmount());
    MerchantSettlementFeeResult feeResult = feeCalculator.calculate(
        grossAmount,
        new MerchantSettlementFeeSnapshot(
            order.getTransactionFeeRate(),
            order.getMinimumFee(),
            order.getFixedFee(),
            order.getGatewayFee(),
            order.getTaxRate(),
            order.getFeeSettlementMode()));
    order.setTransactionFee(feeResult.transactionFee());
    order.setTaxFee(feeResult.taxFee());
    order.setTotalFee(feeResult.totalFee());
    order.setSettlementAmount(feeResult.settlementAmount());

    MerchantBalanceFlowRecord record = buildFlow(
        bizType,
        bizNo,
        order.getMerchantId(),
        order.getMerchantOrderNo(),
        value(order.getToken(), order.getCurrency()),
        order.getChain(),
        order.getTokenAddress(),
        order.getPaymentTxHash(),
        feeResult,
        "普通支付订单入账");
    postCredit(record);
    log.info("普通订单商户余额入账完成。cryptoOrderNo={}, merchantId={}, grossAmount={}, totalFee={}, settlementAmount={}",
        bizNo, order.getMerchantId(), feeResult.grossAmount(), feeResult.totalFee(), feeResult.settlementAmount());
    return Optional.of(record);
  }

  protected Optional<MerchantBalanceFlowRecord> creditSubscriptionBillingLocked(
      SubscriptionOrder order, SubscriptionBillingRecord bill) {
    String bizType = "SUBSCRIPTION_BILLING";
    String bizNo = String.valueOf(bill.getId());
    if (findExisting(bizType, bizNo).isPresent()) {
      log.info("订阅账单商户余额已入账，跳过重复处理。subscriptionOrderNo={}, billingSequence={}",
          order.getSubscriptionOrderNo(), bill.getBillingSequence());
      return Optional.empty();
    }

    BigDecimal grossAmount = value(bill.getRealAmount(), bill.getAmount());
    MerchantSettlementFeeResult feeResult = feeCalculator.calculate(
        grossAmount,
        new MerchantSettlementFeeSnapshot(
            bill.getTransactionFeeRate(),
            bill.getMinimumFee(),
            bill.getFixedFee(),
            bill.getGatewayFee(),
            bill.getTaxRate(),
            bill.getFeeSettlementMode()));
    bill.setTransactionFee(feeResult.transactionFee());
    bill.setTaxFee(feeResult.taxFee());
    bill.setTotalFee(feeResult.totalFee());
    bill.setSettlementAmount(feeResult.settlementAmount());

    MerchantBalanceFlowRecord record = buildFlow(
        bizType,
        bizNo,
        order.getMerchantId(),
        order.getMerchantOrderNo(),
        value(bill.getToken(), bill.getCurrency()),
        bill.getChain(),
        bill.getTokenAddress(),
        bill.getExecutionTxHash(),
        feeResult,
        "订阅周期账单入账");
    postCredit(record);
    log.info("订阅账单商户余额入账完成。subscriptionOrderNo={}, billingSequence={}, merchantId={}, grossAmount={}, totalFee={}, settlementAmount={}",
        order.getSubscriptionOrderNo(),
        bill.getBillingSequence(),
        order.getMerchantId(),
        feeResult.grossAmount(),
        feeResult.totalFee(),
        feeResult.settlementAmount());
    return Optional.of(record);
  }

  private void postCredit(MerchantBalanceFlowRecord record) {
    MerchantBalanceAccount before = findAccount(record.getMerchantId(), record.getBalanceToken());
    record.setBeforeAvailableBalance(
        before == null ? BigDecimal.ZERO : before.getAvailableBalance());
    balanceMapper.credit(record.getMerchantId(), record.getBalanceToken(), record.getNetAmount());
    MerchantBalanceAccount after = findAccount(record.getMerchantId(), record.getBalanceToken());
    if (after == null) {
      throw new IllegalStateException("merchant balance account not found after credit");
    }
    record.setAfterAvailableBalance(after.getAvailableBalance());
    record.setStatus("POSTED");
    record.setCreatedAt(LocalDateTime.now());
    try {
      flowMapper.insert(record);
    } catch (DuplicateKeyException ex) {
      log.info("商户资金流水已存在，回滚本次重复入账。bizType={}, bizNo={}",
          record.getBizType(), record.getBizNo());
      throw ex;
    }
  }

  private Optional<MerchantBalanceFlowRecord> findExisting(String bizType, String bizNo) {
    return Optional.ofNullable(flowMapper.selectOne(Wrappers.<MerchantBalanceFlowRecord>lambdaQuery()
        .eq(MerchantBalanceFlowRecord::getBizType, bizType)
        .eq(MerchantBalanceFlowRecord::getBizNo, bizNo)
        .last("LIMIT 1")));
  }

  private MerchantBalanceAccount findAccount(String merchantId, String token) {
    return balanceMapper.selectOne(Wrappers.<MerchantBalanceAccount>lambdaQuery()
        .eq(MerchantBalanceAccount::getMerchantId, merchantId)
        .eq(MerchantBalanceAccount::getBalanceToken, token)
        .last("LIMIT 1"));
  }

  private MerchantBalanceFlowRecord buildFlow(
      String bizType,
      String bizNo,
      String merchantId,
      String merchantOrderNo,
      String token,
      String chain,
      String tokenAddress,
      String txHash,
      MerchantSettlementFeeResult feeResult,
      String remark) {
    MerchantBalanceFlowRecord record = new MerchantBalanceFlowRecord();
    record.setFlowNo(newFlowNo());
    record.setMerchantId(merchantId);
    record.setBalanceToken(token);
    record.setBizType(bizType);
    record.setBizNo(bizNo);
    record.setMerchantOrderNo(merchantOrderNo);
    record.setChain(chain);
    record.setTokenAddress(tokenAddress);
    record.setTxHash(txHash);
    record.setDirection("CREDIT");
    record.setGrossAmount(feeResult.grossAmount());
    record.setTransactionFee(feeResult.transactionFee());
    record.setFixedFee(feeResult.fixedFee());
    record.setGatewayFee(feeResult.gatewayFee());
    record.setTaxFee(feeResult.taxFee());
    record.setTotalFee(feeResult.totalFee());
    record.setNetAmount(feeResult.settlementAmount());
    record.setRemark(remark);
    return record;
  }

  private String newFlowNo() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    return "MF" + FLOW_TIME_FORMAT.format(LocalDateTime.now()) + suffix;
  }

  private BigDecimal value(BigDecimal preferred, BigDecimal fallback) {
    return preferred == null ? fallback : preferred;
  }

  private String value(String preferred, String fallback) {
    return StringUtils.hasText(preferred) ? preferred : fallback;
  }

  @FunctionalInterface
  private interface LedgerAction {
    Optional<MerchantBalanceFlowRecord> execute();
  }
}
