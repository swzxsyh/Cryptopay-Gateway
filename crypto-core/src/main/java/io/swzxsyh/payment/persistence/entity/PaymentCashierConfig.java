package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 收银台令牌配置。 */
@Data
@TableName("payment_cashier_config")
public class PaymentCashierConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean tokenEncryptionEnabled;
  private String tokenKeyAlias;
  private Integer tokenTtlMinutes;
  private String tokenPrefix;
  private Boolean shortTokenEnabled;
  private String shortTokenPrefix;
  private Integer shortTokenLength;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
