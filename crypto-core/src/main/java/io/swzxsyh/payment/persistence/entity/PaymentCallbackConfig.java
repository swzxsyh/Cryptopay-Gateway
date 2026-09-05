package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 回调重试与死信配置。 */
@Data
@TableName("payment_callback_config")
public class PaymentCallbackConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean retryEnabled;
  private Integer maxAttempts;
  private Long initialBackoffSeconds;
  private Long maxBackoffSeconds;
  private Long retentionDays;
  private Boolean deadLetterEnabled;
  private Boolean dispatchImmediately;
  private Integer dispatchBatchSize;
  private Long dispatchLockWaitMillis;
  private Long dispatchLockLeaseSeconds;
  private Integer maxStoredResponseChars;
  private Integer maxStoredErrorChars;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
