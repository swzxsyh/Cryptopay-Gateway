package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 链扫描 checkpoint。 */
@TableName("chain_scanner_checkpoint")
public class ChainScannerCheckpoint {

  /** 链标识。 */
  @TableId(value = "chain", type = IdType.INPUT)
  private String chain;
  /** 最近扫描到的块高。 */
  private long latestObservedBlock;
  /** 最近确认到的块高。 */
  private long lastConfirmedBlock;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public String getChain() {
    return chain;
  }

  public void setChain(String chain) {
    this.chain = chain;
  }

  public long getLatestObservedBlock() {
    return latestObservedBlock;
  }

  public void setLatestObservedBlock(long latestObservedBlock) {
    this.latestObservedBlock = latestObservedBlock;
  }

  public long getLastConfirmedBlock() {
    return lastConfirmedBlock;
  }

  public void setLastConfirmedBlock(long lastConfirmedBlock) {
    this.lastConfirmedBlock = lastConfirmedBlock;
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
