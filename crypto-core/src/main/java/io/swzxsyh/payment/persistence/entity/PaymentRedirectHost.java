package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 商户跳转地址允许的域名。 */
@Data
@TableName("payment_redirect_host")
public class PaymentRedirectHost {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String host;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
