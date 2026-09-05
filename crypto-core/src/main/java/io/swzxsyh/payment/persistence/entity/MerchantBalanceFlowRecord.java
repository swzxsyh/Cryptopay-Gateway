package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商户资金流水，记录每次余额变动的业务来源、费用明细和变动前后余额。 */
@TableName("merchant_balance_flow_record")
public class MerchantBalanceFlowRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 流水号。 */
  private String flowNo;
  /** 商户号。 */
  private String merchantId;
  /** 余额币种。 */
  private String balanceToken;
  /** 业务类型：PAYMENT_ORDER/SUBSCRIPTION_BILLING 等。 */
  private String bizType;
  /** 业务单号。 */
  private String bizNo;
  /** 商户侧订单号或订阅单号。 */
  private String merchantOrderNo;
  /** 链编码。 */
  private String chain;
  /** 代币合约地址。 */
  private String tokenAddress;
  /** 链上交易哈希。 */
  private String txHash;
  /** 方向：CREDIT/DEBIT/FREEZE/UNFREEZE。 */
  private String direction;
  /** 用户真实支付到账金额。 */
  private BigDecimal grossAmount;
  /** 交易费。 */
  private BigDecimal transactionFee;
  /** 固定手续费。 */
  private BigDecimal fixedFee;
  /** 网关费。 */
  private BigDecimal gatewayFee;
  /** 税费。 */
  private BigDecimal taxFee;
  /** 总费用。 */
  private BigDecimal totalFee;
  /** 商户净入账金额。 */
  private BigDecimal netAmount;
  /** 入账前可用余额。 */
  private BigDecimal beforeAvailableBalance;
  /** 入账后可用余额。 */
  private BigDecimal afterAvailableBalance;
  /** 状态：POSTED。 */
  private String status;
  /** 备注。 */
  private String remark;
  /** 创建时间。 */
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getFlowNo() { return flowNo; }
  public void setFlowNo(String flowNo) { this.flowNo = flowNo; }
  public String getMerchantId() { return merchantId; }
  public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
  public String getBalanceToken() { return balanceToken; }
  public void setBalanceToken(String balanceToken) { this.balanceToken = balanceToken; }
  public String getBizType() { return bizType; }
  public void setBizType(String bizType) { this.bizType = bizType; }
  public String getBizNo() { return bizNo; }
  public void setBizNo(String bizNo) { this.bizNo = bizNo; }
  public String getMerchantOrderNo() { return merchantOrderNo; }
  public void setMerchantOrderNo(String merchantOrderNo) { this.merchantOrderNo = merchantOrderNo; }
  public String getChain() { return chain; }
  public void setChain(String chain) { this.chain = chain; }
  public String getTokenAddress() { return tokenAddress; }
  public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }
  public String getTxHash() { return txHash; }
  public void setTxHash(String txHash) { this.txHash = txHash; }
  public String getDirection() { return direction; }
  public void setDirection(String direction) { this.direction = direction; }
  public BigDecimal getGrossAmount() { return grossAmount; }
  public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
  public BigDecimal getTransactionFee() { return transactionFee; }
  public void setTransactionFee(BigDecimal transactionFee) { this.transactionFee = transactionFee; }
  public BigDecimal getFixedFee() { return fixedFee; }
  public void setFixedFee(BigDecimal fixedFee) { this.fixedFee = fixedFee; }
  public BigDecimal getGatewayFee() { return gatewayFee; }
  public void setGatewayFee(BigDecimal gatewayFee) { this.gatewayFee = gatewayFee; }
  public BigDecimal getTaxFee() { return taxFee; }
  public void setTaxFee(BigDecimal taxFee) { this.taxFee = taxFee; }
  public BigDecimal getTotalFee() { return totalFee; }
  public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee; }
  public BigDecimal getNetAmount() { return netAmount; }
  public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
  public BigDecimal getBeforeAvailableBalance() { return beforeAvailableBalance; }
  public void setBeforeAvailableBalance(BigDecimal beforeAvailableBalance) { this.beforeAvailableBalance = beforeAvailableBalance; }
  public BigDecimal getAfterAvailableBalance() { return afterAvailableBalance; }
  public void setAfterAvailableBalance(BigDecimal afterAvailableBalance) { this.afterAvailableBalance = afterAvailableBalance; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getRemark() { return remark; }
  public void setRemark(String remark) { this.remark = remark; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
