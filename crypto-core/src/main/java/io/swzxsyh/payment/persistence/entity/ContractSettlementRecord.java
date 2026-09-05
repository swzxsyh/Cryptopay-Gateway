package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 智能合约分账总记录。 */
@TableName("contract_settlement_record")
public class ContractSettlementRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 平台支付单号。 */
  private String cryptoOrderNo;
  /** 商户订单号。 */
  private String merchantOrderNo;
  /** 链名。 */
  private String chain;
  /** 币种。 */
  private String token;
  /** 代币合约地址。 */
  private String tokenAddress;
  /** 合约地址。 */
  private String contractAddress;
  /** 金额。 */
  private BigDecimal amount;
  /** 记录状态。 */
  private String status;
  /** 分账模式。 */
  private String settlementMode;
  /** 分账条数。 */
  private Integer splitCount;
  /** basis points 总和。 */
  private Integer totalBasisPoints;
  /** 待签名负载。 */
  private String signPayload;
  /** 签名。 */
  private String signature;
  /** 合约调用参数。 */
  private String contractCallData;
  /** 支付交易哈希。 */
  private String paymentTxHash;
  /** 区块高度。 */
  private Long blockNumber;
  /** 计划时间。 */
  private LocalDateTime plannedAt;
  /** 提交时间。 */
  private LocalDateTime submittedAt;
  /** 结算完成时间。 */
  private LocalDateTime settledAt;
  /** 失败时间。 */
  private LocalDateTime failedAt;
  /** 失败原因。 */
  private String failureReason;
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

  public String getContractAddress() {
    return contractAddress;
  }

  public void setContractAddress(String contractAddress) {
    this.contractAddress = contractAddress;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSettlementMode() {
    return settlementMode;
  }

  public void setSettlementMode(String settlementMode) {
    this.settlementMode = settlementMode;
  }

  public Integer getSplitCount() {
    return splitCount;
  }

  public void setSplitCount(Integer splitCount) {
    this.splitCount = splitCount;
  }

  public Integer getTotalBasisPoints() {
    return totalBasisPoints;
  }

  public void setTotalBasisPoints(Integer totalBasisPoints) {
    this.totalBasisPoints = totalBasisPoints;
  }

  public String getSignPayload() {
    return signPayload;
  }

  public void setSignPayload(String signPayload) {
    this.signPayload = signPayload;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getContractCallData() {
    return contractCallData;
  }

  public void setContractCallData(String contractCallData) {
    this.contractCallData = contractCallData;
  }

  public String getPaymentTxHash() {
    return paymentTxHash;
  }

  public void setPaymentTxHash(String paymentTxHash) {
    this.paymentTxHash = paymentTxHash;
  }

  public Long getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(Long blockNumber) {
    this.blockNumber = blockNumber;
  }

  public LocalDateTime getPlannedAt() {
    return plannedAt;
  }

  public void setPlannedAt(LocalDateTime plannedAt) {
    this.plannedAt = plannedAt;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public LocalDateTime getSettledAt() {
    return settledAt;
  }

  public void setSettledAt(LocalDateTime settledAt) {
    this.settledAt = settledAt;
  }

  public LocalDateTime getFailedAt() {
    return failedAt;
  }

  public void setFailedAt(LocalDateTime failedAt) {
    this.failedAt = failedAt;
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
