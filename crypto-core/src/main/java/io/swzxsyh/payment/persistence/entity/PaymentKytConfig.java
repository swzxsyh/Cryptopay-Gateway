package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** KYT 风控全局配置。 */
@Data
@TableName("payment_kyt_config")
public class PaymentKytConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean kytEnabled;
  private Boolean strictMode;
  private Integer reviewThreshold;
  private Integer rejectThreshold;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
