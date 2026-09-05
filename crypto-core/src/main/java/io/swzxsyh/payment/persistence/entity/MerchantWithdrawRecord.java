package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商户提现流水，记录申请、审核、链上提交和最终状态。 */
@TableName("merchant_withdraw_record")
public class MerchantWithdrawRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 提现单号。 */
  private String withdrawNo;
  /** 商户号。 */
  private String merchantId;
  /** 提现币种。 */
  private String token;
  /** 提现金额。 */
  private BigDecimal amount;
  /** 提现链。 */
  private String chain;
  /** 提现收款地址。 */
  private String withdrawAddress;
  /** 链上交易哈希。 */
  private String txHash;
  /** 状态：SUBMITTED/APPROVED/REJECTED/PROCESSING/SUCCEEDED/FAILED。 */
  private String status;
  /** 操作人。 */
  private String operator;
  /** 操作备注。 */
  private String operatorNote;
  /** 失败原因。 */
  private String failureReason;
  /** 申请时间。 */
  private LocalDateTime requestedAt;
  /** 审核时间。 */
  private LocalDateTime reviewedAt;
  /** 提交链上时间。 */
  private LocalDateTime submittedAt;
  /** 完成时间。 */
  private LocalDateTime completedAt;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getWithdrawNo() { return withdrawNo; }
  public void setWithdrawNo(String withdrawNo) { this.withdrawNo = withdrawNo; }
  public String getMerchantId() { return merchantId; }
  public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getChain() { return chain; }
  public void setChain(String chain) { this.chain = chain; }
  public String getWithdrawAddress() { return withdrawAddress; }
  public void setWithdrawAddress(String withdrawAddress) { this.withdrawAddress = withdrawAddress; }
  public String getTxHash() { return txHash; }
  public void setTxHash(String txHash) { this.txHash = txHash; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getOperator() { return operator; }
  public void setOperator(String operator) { this.operator = operator; }
  public String getOperatorNote() { return operatorNote; }
  public void setOperatorNote(String operatorNote) { this.operatorNote = operatorNote; }
  public String getFailureReason() { return failureReason; }
  public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
  public LocalDateTime getRequestedAt() { return requestedAt; }
  public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
  public LocalDateTime getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
  public LocalDateTime getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
  public LocalDateTime getCompletedAt() { return completedAt; }
  public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
