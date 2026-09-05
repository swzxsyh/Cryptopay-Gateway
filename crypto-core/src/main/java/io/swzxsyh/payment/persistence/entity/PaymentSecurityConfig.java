package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 开放重定向等安全策略配置。 */
@Data
@TableName("payment_security_config")
public class PaymentSecurityConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean validateRedirectUrl;
  private Boolean allowLocalRedirect;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
