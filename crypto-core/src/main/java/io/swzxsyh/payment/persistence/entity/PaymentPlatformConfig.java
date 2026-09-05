package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 平台级支付基础配置。 */
@Data
@TableName("payment_platform_config")
public class PaymentPlatformConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Integer orderExpireMinutes;
  private String cashierBaseUrl;
  private String treasuryAddress;
  private Long idempotencyWaitMillis;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
