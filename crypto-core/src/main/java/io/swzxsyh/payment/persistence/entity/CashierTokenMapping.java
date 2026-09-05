package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 收银台短码到加密 token 的映射记录。 */
@Data
@TableName("cashier_token_mapping")
public class CashierTokenMapping {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String shortCode;
  private String encryptedToken;
  private String cryptoOrderNo;
  private String merchantId;
  private LocalDateTime expireAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
