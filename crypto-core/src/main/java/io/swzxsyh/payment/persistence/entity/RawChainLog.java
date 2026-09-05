package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 原始链上流水，保存扫描器发现的链上入账事实，便于运营排查和人工补单。 */
@TableName("raw_chain_logs")
public class RawChainLog {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 链编码。 */
  private String chain;
  /** 交易哈希。 */
  private String txHash;
  /** 同一交易内的日志序号，原生币或无日志序号时为 0。 */
  private Long logIndex;
  /** 扫描来源。 */
  private String source;
  /** 币种。 */
  private String token;
  /** 币种合约地址。 */
  private String tokenAddress;
  /** 付款地址。 */
  private String fromAddress;
  /** 收款地址。 */
  private String toAddress;
  /** 实际转账金额。 */
  private BigDecimal amount;
  /** 区块高度或 slot。 */
  private Long blockNumber;
  /** 链上区块时间；拿不到时使用扫描观察时间。 */
  private LocalDateTime blockTimestamp;
  /** 当前处理状态。 */
  private String status;
  /** 匹配到的平台订单号。 */
  private String matchedCryptoOrderNo;
  /** 匹配说明或失败原因。 */
  private String matchReason;
  /** 运营处理人。 */
  private String operator;
  /** 运营处理备注。 */
  private String operatorNote;
  /** 运营处理时间。 */
  private LocalDateTime manualProcessedAt;
  /** 扫描观察时间。 */
  private LocalDateTime observedAt;
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

  public String getChain() {
    return chain;
  }

  public void setChain(String chain) {
    this.chain = chain;
  }

  public String getTxHash() {
    return txHash;
  }

  public void setTxHash(String txHash) {
    this.txHash = txHash;
  }

  public Long getLogIndex() {
    return logIndex;
  }

  public void setLogIndex(Long logIndex) {
    this.logIndex = logIndex;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
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

  public String getFromAddress() {
    return fromAddress;
  }

  public void setFromAddress(String fromAddress) {
    this.fromAddress = fromAddress;
  }

  public String getToAddress() {
    return toAddress;
  }

  public void setToAddress(String toAddress) {
    this.toAddress = toAddress;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Long getBlockNumber() {
    return blockNumber;
  }

  public void setBlockNumber(Long blockNumber) {
    this.blockNumber = blockNumber;
  }

  public LocalDateTime getBlockTimestamp() {
    return blockTimestamp;
  }

  public void setBlockTimestamp(LocalDateTime blockTimestamp) {
    this.blockTimestamp = blockTimestamp;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMatchedCryptoOrderNo() {
    return matchedCryptoOrderNo;
  }

  public void setMatchedCryptoOrderNo(String matchedCryptoOrderNo) {
    this.matchedCryptoOrderNo = matchedCryptoOrderNo;
  }

  public String getMatchReason() {
    return matchReason;
  }

  public void setMatchReason(String matchReason) {
    this.matchReason = matchReason;
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

  public LocalDateTime getManualProcessedAt() {
    return manualProcessedAt;
  }

  public void setManualProcessedAt(LocalDateTime manualProcessedAt) {
    this.manualProcessedAt = manualProcessedAt;
  }

  public LocalDateTime getObservedAt() {
    return observedAt;
  }

  public void setObservedAt(LocalDateTime observedAt) {
    this.observedAt = observedAt;
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
