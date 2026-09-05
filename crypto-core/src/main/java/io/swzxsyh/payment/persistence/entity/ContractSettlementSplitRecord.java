package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 智能合约分账明细。 */
@TableName("contract_settlement_split_record")
public class ContractSettlementSplitRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 关联总记录 ID。 */
  private Long settlementRecordId;
  /** 平台支付单号。 */
  private String cryptoOrderNo;
  /** 分账角色。 */
  private String role;
  /** 收款地址。 */
  private String receiver;
  /** 分账比例。 */
  private Integer basisPoints;
  /** 分账金额。 */
  private BigDecimal amount;
  /** 分账状态。 */
  private String status;
  /** 交易哈希。 */
  private String paymentTxHash;
  /** 区块高度。 */
  private Long blockNumber;
  /** 执行时间。 */
  private LocalDateTime executedAt;
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

  public Long getSettlementRecordId() {
    return settlementRecordId;
  }

  public void setSettlementRecordId(Long settlementRecordId) {
    this.settlementRecordId = settlementRecordId;
  }

  public String getCryptoOrderNo() {
    return cryptoOrderNo;
  }

  public void setCryptoOrderNo(String cryptoOrderNo) {
    this.cryptoOrderNo = cryptoOrderNo;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  public Integer getBasisPoints() {
    return basisPoints;
  }

  public void setBasisPoints(Integer basisPoints) {
    this.basisPoints = basisPoints;
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

  public LocalDateTime getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(LocalDateTime executedAt) {
    this.executedAt = executedAt;
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
