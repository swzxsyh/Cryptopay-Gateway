package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** KYT 高风险 Token 规则。 */
@Data
@TableName("payment_kyt_token_rule")
public class PaymentKytTokenRule {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String tokenSymbol;
  private String chainCode;
  private String ruleType;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
