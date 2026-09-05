package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 支付支持的 Token 配置。 */
@Data
@TableName("payment_token_config")
public class PaymentTokenConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String chainCode;
  private String tokenSymbol;
  private String tokenAddress;
  private Integer decimals;
  private Integer confirmationDepth;
  private Boolean enabled;
  private Integer sortNo;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
