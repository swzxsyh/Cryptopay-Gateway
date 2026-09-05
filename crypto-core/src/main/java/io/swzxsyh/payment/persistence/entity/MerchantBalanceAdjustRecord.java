package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商户余额人工调账记录，manager 端只能通过该表追踪每次余额修正。 */
@TableName("merchant_balance_adjust_record")
public class MerchantBalanceAdjustRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 调账单号。 */
  private String adjustNo;
  /** 商户号。 */
  private String merchantId;
  /** 币种。 */
  private String token;
  /** 方向：CREDIT 增加余额，DEBIT 扣减余额。 */
  private String direction;
  /** 调账金额。 */
  private BigDecimal amount;
  /** 调账原因。 */
  private String reason;
  /** 操作人。 */
  private String operator;
  /** 操作备注。 */
  private String operatorNote;
  /** 调账前可用余额。 */
  private BigDecimal beforeAvailableBalance;
  /** 调账后可用余额。 */
  private BigDecimal afterAvailableBalance;
  /** 创建时间。 */
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getAdjustNo() { return adjustNo; }
  public void setAdjustNo(String adjustNo) { this.adjustNo = adjustNo; }
  public String getMerchantId() { return merchantId; }
  public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getOperator() { return operator; }
  public void setOperator(String operator) { this.operator = operator; }
  public String getOperatorNote() { return operatorNote; }
  public void setOperatorNote(String operatorNote) { this.operatorNote = operatorNote; }
  public BigDecimal getBeforeAvailableBalance() { return beforeAvailableBalance; }
  public void setBeforeAvailableBalance(BigDecimal beforeAvailableBalance) { this.beforeAvailableBalance = beforeAvailableBalance; }
  public BigDecimal getAfterAvailableBalance() { return afterAvailableBalance; }
  public void setAfterAvailableBalance(BigDecimal afterAvailableBalance) { this.afterAvailableBalance = afterAvailableBalance; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
