package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 订阅支付默认配置。 */
@Data
@TableName("payment_subscription_config")
public class PaymentSubscriptionConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean subscriptionEnabled;
  private String defaultMode;
  private Integer defaultCycleSeconds;
  private String superfluidHostAddress;
  private String superfluidCfaAddress;
  private String erc1337ExecutorAddress;
  private Boolean schedulerEnabled;
  private Integer schedulerIntervalSeconds;
  private Integer streamSettlementIntervalSeconds;
  private Integer schedulerBatchSize;
  private Integer maxRetryCount;
  private Integer retryBackoffSeconds;
  private java.math.BigInteger executionGasLimit;
  private String executorPrivateKeySourceType;
  private String executorPrivateKeyEnv;
  private String executorPrivateKeyKmsKeyId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
