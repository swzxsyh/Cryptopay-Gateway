package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 链扫描全局策略配置。 */
@Data
@TableName("payment_scanner_config")
public class PaymentScannerConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
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
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
