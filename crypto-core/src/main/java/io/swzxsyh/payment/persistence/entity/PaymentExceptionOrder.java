package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付异常单，用于承接逾期支付、争议支付等需要运营人工处理的链上事实。 */
@TableName("payment_exception_order")
public class PaymentExceptionOrder {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 异常单号。 */
  private String exceptionNo;
  /** 异常类型。 */
  private String exceptionType;
  /** 异常单状态。 */
  private String status;
  /** 商户号。 */
  private String merchantId;
  /** 商户订单号。 */
  private String merchantOrderNo;
  /** 平台支付单号。 */
  private String cryptoOrderNo;
  /** 链编码。 */
  private String chain;
  /** 币种。 */
  private String token;
  /** 币种合约地址。 */
  private String tokenAddress;
  /** 收款地址。 */
  private String paymentAddress;
  /** 付款地址。 */
  private String sourceAddress;
  /** 链上交易哈希。 */
  private String txHash;
  /** 订单应付金额。 */
  private BigDecimal expectedAmount;
  /** 实际到账金额。 */
  private BigDecimal realAmount;
  /** 入账所在区块或 slot。 */
  private Long blockNumber;
  /** 创建异常单时原订单状态。 */
  private String orderStatus;
  /** 异常原因说明。 */
  private String reason;
  /** 处理人。 */
  private String operator;
  /** 处理备注。 */
  private String operatorNote;
  /** 处理时间。 */
  private LocalDateTime handledAt;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getExceptionNo() {
    return exceptionNo;
  }

  public void setExceptionNo(String exceptionNo) {
    this.exceptionNo = exceptionNo;
  }

  public String getExceptionType() {
    return exceptionType;
  }

  public void setExceptionType(String exceptionType) {
    this.exceptionType = exceptionType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public String getCryptoOrderNo() {
    return cryptoOrderNo;
  }

  public void setCryptoOrderNo(String cryptoOrderNo) {
    this.cryptoOrderNo = cryptoOrderNo;
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

  public String getPaymentAddress() {
    return paymentAddress;
  }

  public void setPaymentAddress(String paymentAddress) {
    this.paymentAddress = paymentAddress;
  }

  public String getSourceAddress() {
    return sourceAddress;
  }

  public void setSourceAddress(String sourceAddress) {
    this.sourceAddress = sourceAddress;
  }

  public String getTxHash() {
    return txHash;
  }

  public void setTxHash(String txHash) {
    this.txHash = txHash;
  }

  public BigDecimal getExpectedAmount() {
    return expectedAmount;
  }

  public void setExpectedAmount(BigDecimal expectedAmount) {
    this.expectedAmount = expectedAmount;
  }

  public BigDecimal getRealAmount() {
    return realAmount;
  }

  public void setRealAmount(BigDecimal realAmount) {
    this.realAmount = realAmount;
  }

  public Long getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(Long blockNumber) {
    this.blockNumber = blockNumber;
  }

  public String getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(String orderStatus) {
    this.orderStatus = orderStatus;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getOperatorNote() {
    return operatorNote;
  }

  public void setOperatorNote(String operatorNote) {
    this.operatorNote = operatorNote;
  }

  public LocalDateTime getHandledAt() {
    return handledAt;
  }

  public void setHandledAt(LocalDateTime handledAt) {
    this.handledAt = handledAt;
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
