package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** x402 网关及服务费配置。 */
@Data
@TableName("payment_gateway_config")
public class PaymentGatewayConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean gatewayEnabled;
  private String serviceFeeToken;
  private BigDecimal serviceFeeAmount;
  private String apiKey;
  private Boolean publicResourcesEnabled;
  private String facilitatorName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
