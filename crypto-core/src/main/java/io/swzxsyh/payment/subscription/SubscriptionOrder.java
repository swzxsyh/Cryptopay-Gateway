package io.swzxsyh.payment.subscription;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("subscription_order")
/**
 * 订阅订单实体。
 *
 * 记录订阅类支付的创建参数、编排结果、后续扣款节奏和状态流转。
 */
public class SubscriptionOrder {

  /** 订阅单号。 */
  @TableId(value = "subscription_order_no", type = IdType.INPUT)
  private String subscriptionOrderNo;
  /** 商户侧订阅单号。 */
  private String merchantOrderNo;
  /** 商户标识。 */
  private String merchantId;
  /** 单周期扣款金额。 */
  private BigDecimal amountPerCycle;
  /** 币种。 */
  private String currency;
  /** 订阅链。 */
  private String chain;
  /** 订阅币种。 */
  private String token;
  /** 代币合约地址。 */
  private String tokenAddress;
  /** 付款方地址。 */
  private String payerAddress;
  /** 收款方地址。 */
  private String recipientAddress;
  /** 订阅编排模式。 */
  private SubscriptionBillingMode billingMode;
  /** 周期秒数。 */
  private int cycleSeconds;
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
  /** 费用结算粒度：PER_BILLING/PER_STREAM_WINDOW/MONTHLY_AGGREGATED。 */
  private String feeSettlementMode;
  /** 通知地址。 */
  private String notifyUrl;
  /** 返回地址。 */
  private String returnUrl;
  /** 订阅准备阶段的合约地址。 */
  private String setupContractAddress;
  /** 订阅准备阶段的调用参数。 */
  private String setupPayload;
  /** 订阅策略选择原因。 */
  private String setupReason;
  /** 初始化订阅的链上交易哈希。 */
  private String setupTxHash;
  /** 链上事件中的订阅 ID，通常为订阅单号 hash 后的 bytes32。 */
  private String subscriptionEventId;
  /** 当前状态。 */
  private SubscriptionStatus status;
  /** 下次扣款时间。 */
  private LocalDateTime nextBillingAt;
  /** 最近一次扣款时间。 */
  private LocalDateTime lastBillingAt;
  /** 订阅激活时间。 */
  private LocalDateTime activatedAt;
  /** 订阅暂停时间。 */
  private LocalDateTime pausedAt;
  /** 订阅取消时间。 */
  private LocalDateTime cancelledAt;
  /** 最近失败原因。 */
  private String failureReason;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public String getSubscriptionOrderNo() {
    return subscriptionOrderNo;
  }

  public void setSubscriptionOrderNo(String subscriptionOrderNo) {
    this.subscriptionOrderNo = subscriptionOrderNo;
  }

  public String getMerchantOrderNo() {
    return merchantOrderNo;
  }

  public void setMerchantOrderNo(String merchantOrderNo) {
    this.merchantOrderNo = merchantOrderNo;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public BigDecimal getAmountPerCycle() {
    return amountPerCycle;
  }

  public void setAmountPerCycle(BigDecimal amountPerCycle) {
    this.amountPerCycle = amountPerCycle;
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

  public String getPayerAddress() {
    return payerAddress;
  }

  public void setPayerAddress(String payerAddress) {
    this.payerAddress = payerAddress;
  }

  public String getRecipientAddress() {
    return recipientAddress;
  }

  public void setRecipientAddress(String recipientAddress) {
    this.recipientAddress = recipientAddress;
  }

  public SubscriptionBillingMode getBillingMode() {
    return billingMode;
  }

  public void setBillingMode(SubscriptionBillingMode billingMode) {
    this.billingMode = billingMode;
  }

  public int getCycleSeconds() {
    return cycleSeconds;
  }

  public void setCycleSeconds(int cycleSeconds) {
    this.cycleSeconds = cycleSeconds;
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

  public String getNotifyUrl() {
    return notifyUrl;
  }

  public void setNotifyUrl(String notifyUrl) {
    this.notifyUrl = notifyUrl;
  }

  public String getReturnUrl() {
    return returnUrl;
  }

  public void setReturnUrl(String returnUrl) {
    this.returnUrl = returnUrl;
  }

  public String getSetupContractAddress() {
    return setupContractAddress;
  }

  public void setSetupContractAddress(String setupContractAddress) {
    this.setupContractAddress = setupContractAddress;
  }

  public String getSetupPayload() {
    return setupPayload;
  }

  public void setSetupPayload(String setupPayload) {
    this.setupPayload = setupPayload;
  }

  public String getSetupReason() {
    return setupReason;
  }

  public void setSetupReason(String setupReason) {
    this.setupReason = setupReason;
  }

  public String getSetupTxHash() {
    return setupTxHash;
  }

  public void setSetupTxHash(String setupTxHash) {
    this.setupTxHash = setupTxHash;
  }

  public String getSubscriptionEventId() {
    return subscriptionEventId;
  }

  public void setSubscriptionEventId(String subscriptionEventId) {
    this.subscriptionEventId = subscriptionEventId;
  }

  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus status) {
    this.status = status;
  }

  public LocalDateTime getNextBillingAt() {
    return nextBillingAt;
  }

  public void setNextBillingAt(LocalDateTime nextBillingAt) {
    this.nextBillingAt = nextBillingAt;
  }

  public LocalDateTime getLastBillingAt() {
    return lastBillingAt;
  }

  public void setLastBillingAt(LocalDateTime lastBillingAt) {
    this.lastBillingAt = lastBillingAt;
  }

  public LocalDateTime getActivatedAt() {
    return activatedAt;
  }

  public void setActivatedAt(LocalDateTime activatedAt) {
    this.activatedAt = activatedAt;
  }

  public LocalDateTime getPausedAt() {
    return pausedAt;
  }

  public void setPausedAt(LocalDateTime pausedAt) {
    this.pausedAt = pausedAt;
  }

  public LocalDateTime getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(LocalDateTime cancelledAt) {
    this.cancelledAt = cancelledAt;
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
}
