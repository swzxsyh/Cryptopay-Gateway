package io.swzxsyh.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 主支付订单实体。
 *
 * 这张表承载普通下单、收银台选择支付方式、链上付款结果、回调数据和结算辅助信息。
 */
@TableName("payment_order")
public class PaymentOrder {

  /** 平台内部支付单号。 */
  @TableId(value = "crypto_order_no", type = IdType.INPUT)
  private String cryptoOrderNo;
  /** 商户号。 */
  private String merchantId;
  /** 商户侧订单号。 */
  private String merchantOrderNo;
  /** 订单应支付金额。 */
  private BigDecimal amount;
  /** 订单币种，如 USDT / USDC。 */
  private String currency;
  /** 用户最终选择的链。 */
  private String chain;
  /** 用户最终选择的币种标识。 */
  private String token;
  /** 代币合约地址。 */
  private String tokenAddress;
  /** 用户钱包地址。 */
  private String walletAddress;
  /** 用户钱包类型，EOA / SCA / UNKNOWN。 */
  private String walletAccountType;
  /** 最终选定的支付方式。 */
  private PaymentMethod paymentMethod;
  /** 路由类型，用于记录为什么走这条支付链路。 */
  private String tokenRouteType;
  /** 路由解释原因。 */
  private String routeReason;
  /** 当前订单状态。 */
  private OrderStatus status;
  /** 自由转账模式下的专属收款地址。 */
  private String paymentAddress;
  /** 智能合约模式下的合约地址。 */
  private String contractAddress;
  /** 智能合约模式下的调用参数。 */
  private String contractCallData;
  /** Gas 责任方模式。 */
  private String gasPayerMode;
  /** Gas 策略说明。 */
  private String gasReason;
  /** 预估 Gas 费用（Wei）。 */
  private java.math.BigDecimal gasEstimatedFeeWei;
  /** 客户钱包余额是否足以支付 Gas。 */
  private boolean gasCustomerBalanceSufficient;
  /** 平台钱包余额是否足以代付 Gas。 */
  private boolean gasPlatformBalanceSufficient;
  /** Gas 兜底建议。 */
  private String gasFallbackSuggestion;
  /** 派生地址池标识。 */
  private String derivedAddressPoolKey;
  /** 派生地址租约 ID。 */
  private String derivedAddressLeaseId;
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
  /** 费用结算粒度：PER_ORDER/PER_BILLING/PER_STREAM_WINDOW/MONTHLY_AGGREGATED。 */
  private String feeSettlementMode;
  /** 实际交易费。 */
  private BigDecimal transactionFee;
  /** 实际税费。 */
  private BigDecimal taxFee;
  /** 实际总费用。 */
  private BigDecimal totalFee;
  /** 商户净入账金额。 */
  private BigDecimal settlementAmount;
  /** 链上支付交易哈希。 */
  private String paymentTxHash;
  /** 实际到账金额。 */
  private BigDecimal realAmount;
  /** 实际支付完成时间。 */
  private LocalDateTime paidAt;
  /** 是否为超时后的延迟到账。 */
  private boolean latePayment;
  /** 延迟到账时间。 */
  private LocalDateTime latePaymentAt;
  /** 商户通知地址。 */
  private String notifyUrl;
  /** 商户跳转地址。 */
  private String returnUrl;
  /** 订单过期时间。 */
  private LocalDateTime expireTime;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public String getCryptoOrderNo() {
    return cryptoOrderNo;
  }

  public void setCryptoOrderNo(String cryptoOrderNo) {
    this.cryptoOrderNo = cryptoOrderNo;
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

  public String getWalletAddress() {
    return walletAddress;
  }

  public void setWalletAddress(String walletAddress) {
    this.walletAddress = walletAddress;
  }

  public String getWalletAccountType() {
    return walletAccountType;
  }

  public void setWalletAccountType(String walletAccountType) {
    this.walletAccountType = walletAccountType;
  }

  public PaymentMethod getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(PaymentMethod paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getTokenRouteType() {
    return tokenRouteType;
  }

  public void setTokenRouteType(String tokenRouteType) {
    this.tokenRouteType = tokenRouteType;
  }

  public String getRouteReason() {
    return routeReason;
  }

  public void setRouteReason(String routeReason) {
    this.routeReason = routeReason;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public String getPaymentAddress() {
    return paymentAddress;
  }

  public void setPaymentAddress(String paymentAddress) {
    this.paymentAddress = paymentAddress;
  }

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public String getContractCallData() {
    return contractCallData;
  }

  public void setContractCallData(String contractCallData) {
    this.contractCallData = contractCallData;
  }

  public String getGasPayerMode() {
    return gasPayerMode;
  }

  public void setGasPayerMode(String gasPayerMode) {
    this.gasPayerMode = gasPayerMode;
  }

  public String getGasReason() {
    return gasReason;
  }

  public void setGasReason(String gasReason) {
    this.gasReason = gasReason;
  }

  public java.math.BigDecimal getGasEstimatedFeeWei() {
    return gasEstimatedFeeWei;
  }

  public void setGasEstimatedFeeWei(java.math.BigDecimal gasEstimatedFeeWei) {
    this.gasEstimatedFeeWei = gasEstimatedFeeWei;
  }

  public boolean isGasCustomerBalanceSufficient() {
    return gasCustomerBalanceSufficient;
  }

  public void setGasCustomerBalanceSufficient(boolean gasCustomerBalanceSufficient) {
    this.gasCustomerBalanceSufficient = gasCustomerBalanceSufficient;
  }

  public boolean isGasPlatformBalanceSufficient() {
    return gasPlatformBalanceSufficient;
  }

  public void setGasPlatformBalanceSufficient(boolean gasPlatformBalanceSufficient) {
    this.gasPlatformBalanceSufficient = gasPlatformBalanceSufficient;
  }

  public String getGasFallbackSuggestion() {
    return gasFallbackSuggestion;
  }

  public void setGasFallbackSuggestion(String gasFallbackSuggestion) {
    this.gasFallbackSuggestion = gasFallbackSuggestion;
  }

  public String getDerivedAddressPoolKey() {
    return derivedAddressPoolKey;
  }

  public void setDerivedAddressPoolKey(String derivedAddressPoolKey) {
    this.derivedAddressPoolKey = derivedAddressPoolKey;
  }

  public String getDerivedAddressLeaseId() {
    return derivedAddressLeaseId;
  }

  public void setDerivedAddressLeaseId(String derivedAddressLeaseId) {
    this.derivedAddressLeaseId = derivedAddressLeaseId;
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

  public String getPaymentTxHash() {
    return paymentTxHash;
  }

  public void setPaymentTxHash(String paymentTxHash) {
    this.paymentTxHash = paymentTxHash;
  }

  public BigDecimal getRealAmount() {
    return realAmount;
  }

  public void setRealAmount(BigDecimal realAmount) {
    this.realAmount = realAmount;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public boolean isLatePayment() {
    return latePayment;
  }

  public void setLatePayment(boolean latePayment) {
    this.latePayment = latePayment;
  }

  public LocalDateTime getLatePaymentAt() {
    return latePaymentAt;
  }

  public void setLatePaymentAt(LocalDateTime latePaymentAt) {
    this.latePaymentAt = latePaymentAt;
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

  public LocalDateTime getExpireTime() {
    return expireTime;
  }

  public void setExpireTime(LocalDateTime expireTime) {
    this.expireTime = expireTime;
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
