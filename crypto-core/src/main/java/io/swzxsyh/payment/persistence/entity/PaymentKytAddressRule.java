package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** KYT 地址黑白名单规则。 */
@Data
@TableName("payment_kyt_address_rule")
public class PaymentKytAddressRule {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String address;
  private String ruleType;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
