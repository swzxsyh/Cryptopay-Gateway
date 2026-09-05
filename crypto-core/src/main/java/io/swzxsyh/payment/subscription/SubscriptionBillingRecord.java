package io.swzxsyh.payment.subscription;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订阅周期账单实体。
 *
 * <p>每一个订阅周期都会生成一条账单，用来承载链上扣款交易、确认结果、回调追踪和补偿重试。
 */
@TableName("subscription_billing_record")
public class SubscriptionBillingRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 平台订阅订单号。 */
  private String subscriptionOrderNo;
  /** 商户标识。 */
  private String merchantId;
  /** 商户侧订阅单号。 */
  private String merchantOrderNo;
  /** 账单期数，从 1 开始递增。 */
  private Integer billingSequence;
  /** 本期应扣金额。 */
  private BigDecimal amount;
  /** 币种。 */
  private String currency;
  /** 链编码。 */
  private String chain;
  /** 代币符号。 */
  private String token;
  /** 代币合约地址。 */
  private String tokenAddress;
  /** 本期计划扣款时间。 */
  private LocalDateTime dueAt;
  /** 当前账单状态。 */
  private SubscriptionBillingStatus status;
  /** 链上执行交易哈希。 */
  private String executionTxHash;
  /** 链上确认区块。 */
  private Long confirmedBlockNumber;
  /** 实际到账/扣款金额。 */
  private BigDecimal realAmount;
  /** 交易费率快照。 */
  private BigDecimal transactionFeeRate;
  /** 保底手续费快照。 */
  private BigDecimal minimumFee;
  /** 固定手续费快照。 */
  private BigDecimal fixedFee;
  /** 网关费快照。 */
  private BigDecimal gatewayFee;
  /** 税率快照。 */
  private BigDecimal taxRate;
  /** 费用结算粒度快照。 */
  private String feeSettlementMode;
  /** 实际交易费。 */
  private BigDecimal transactionFee;
  /** 实际税费。 */
  private BigDecimal taxFee;
  /** 实际总费用。 */
  private BigDecimal totalFee;
  /** 商户净入账金额。 */
  private BigDecimal settlementAmount;
  /** 已重试次数。 */
  private Integer retryCount;
  /** 下次重试时间。 */
  private LocalDateTime nextRetryAt;
  /** 最近失败原因。 */
  private String failureReason;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;
  /** 支付确认时间。 */
  private LocalDateTime paidAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSubscriptionOrderNo() {
    return subscriptionOrderNo;
  }

  public void setSubscriptionOrderNo(String subscriptionOrderNo) {
    this.subscriptionOrderNo = subscriptionOrderNo;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getMerchantOrderNo() {
    return merchantOrderNo;
  }

  public void setMerchantOrderNo(String merchantOrderNo) {
    this.merchantOrderNo = merchantOrderNo;
  }

  public Integer getBillingSequence() {
    return billingSequence;
  }

  public void setBillingSequence(Integer billingSequence) {
    this.billingSequence = billingSequence;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getChain() {
    return chain;
  }

  public void setChain(String chain) {
    this.chain = chain;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getTokenAddress() {
    return tokenAddress;
  }

  public void setTokenAddress(String tokenAddress) {
    this.tokenAddress = tokenAddress;
  }

  public LocalDateTime getDueAt() {
    return dueAt;
  }

  public void setDueAt(LocalDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public SubscriptionBillingStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionBillingStatus status) {
    this.status = status;
  }

  public String getExecutionTxHash() {
    return executionTxHash;
  }

  public void setExecutionTxHash(String executionTxHash) {
    this.executionTxHash = executionTxHash;
  }

  public Long getConfirmedBlockNumber() {
    return confirmedBlockNumber;
  }

  public void setConfirmedBlockNumber(Long confirmedBlockNumber) {
    this.confirmedBlockNumber = confirmedBlockNumber;
  }

  public BigDecimal getRealAmount() {
    return realAmount;
  }

  public void setRealAmount(BigDecimal realAmount) {
    this.realAmount = realAmount;
  }

  public BigDecimal getTransactionFeeRate() {
    return transactionFeeRate;
  }

  public void setTransactionFeeRate(BigDecimal transactionFeeRate) {
    this.transactionFeeRate = transactionFeeRate;
  }

  public BigDecimal getMinimumFee() {
    return minimumFee;
  }

  public void setMinimumFee(BigDecimal minimumFee) {
    this.minimumFee = minimumFee;
  }

  public BigDecimal getFixedFee() {
    return fixedFee;
  }

  public void setFixedFee(BigDecimal fixedFee) {
    this.fixedFee = fixedFee;
  }

  public BigDecimal getGatewayFee() {
    return gatewayFee;
  }

  public void setGatewayFee(BigDecimal gatewayFee) {
    this.gatewayFee = gatewayFee;
  }

  public BigDecimal getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public String getFeeSettlementMode() {
    return feeSettlementMode;
  }

  public void setFeeSettlementMode(String feeSettlementMode) {
    this.feeSettlementMode = feeSettlementMode;
  }

  public BigDecimal getTransactionFee() {
    return transactionFee;
  }

  public void setTransactionFee(BigDecimal transactionFee) {
    this.transactionFee = transactionFee;
  }

  public BigDecimal getTaxFee() {
    return taxFee;
  }

  public void setTaxFee(BigDecimal taxFee) {
    this.taxFee = taxFee;
  }

  public BigDecimal getTotalFee() {
    return totalFee;
  }

  public void setTotalFee(BigDecimal totalFee) {
    this.totalFee = totalFee;
  }

  public BigDecimal getSettlementAmount() {
    return settlementAmount;
  }

  public void setSettlementAmount(BigDecimal settlementAmount) {
    this.settlementAmount = settlementAmount;
  }

  public Integer getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(Integer retryCount) {
    this.retryCount = retryCount;
  }

  public LocalDateTime getNextRetryAt() {
    return nextRetryAt;
  }

  public void setNextRetryAt(LocalDateTime nextRetryAt) {
    this.nextRetryAt = nextRetryAt;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }
}
