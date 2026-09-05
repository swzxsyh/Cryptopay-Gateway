package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 能力发现接口配置。 */
@Data
@TableName("payment_discovery_config")
public class PaymentDiscoveryConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean discoveryEnabled;
  private String publicBaseUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
