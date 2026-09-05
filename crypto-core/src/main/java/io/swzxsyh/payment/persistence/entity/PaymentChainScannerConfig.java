package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 单链扫描策略覆盖配置；字段为空时使用全局 payment_scanner_config。 */
@Data
@TableName("payment_chain_scanner_config")
public class PaymentChainScannerConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String chainCode;
  private Integer confirmationDepth;
  private Integer scanIntervalSeconds;
  private Integer checkpointFlushBlocks;
  private Integer backfillBlocks;
  private Integer logScanBatchBlocks;
  private Integer logScanRetryAttempts;
  private Integer failureCooldownSeconds;
  private Boolean websocketEnabled;
  private Integer websocketLeaderLeaseSeconds;
  private Long chainReplayTtlHours;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
