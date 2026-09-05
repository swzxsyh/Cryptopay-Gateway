package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商户资金余额账户，用于记录可提现资金，不与服务费余额混用。 */
@TableName("merchant_balance_account")
public class MerchantBalanceAccount {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 商户号。 */
  private String merchantId;
  /** 余额币种。 */
  private String balanceToken;
  /** 可用余额。 */
  private BigDecimal availableBalance;
  /** 提现中冻结余额。 */
  private BigDecimal frozenBalance;
  /** 累计入账金额。 */
  private BigDecimal totalIncome;
  /** 累计提现成功金额。 */
  private BigDecimal totalWithdrawn;
  /** 状态：ENABLED/DISABLED。 */
  private String status;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getMerchantId() { return merchantId; }
  public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
  public String getBalanceToken() { return balanceToken; }
  public void setBalanceToken(String balanceToken) { this.balanceToken = balanceToken; }
  public BigDecimal getAvailableBalance() { return availableBalance; }
  public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
  public BigDecimal getFrozenBalance() { return frozenBalance; }
  public void setFrozenBalance(BigDecimal frozenBalance) { this.frozenBalance = frozenBalance; }
  public BigDecimal getTotalIncome() { return totalIncome; }
  public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }
  public BigDecimal getTotalWithdrawn() { return totalWithdrawn; }
  public void setTotalWithdrawn(BigDecimal totalWithdrawn) { this.totalWithdrawn = totalWithdrawn; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
