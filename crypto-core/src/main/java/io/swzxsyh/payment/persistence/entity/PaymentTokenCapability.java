package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Token 协议能力配置。 */
@Data
@TableName("payment_token_capability")
public class PaymentTokenCapability {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String chainCode;
  private String tokenSymbol;
  private Boolean transferWithAuthorization;
  private Boolean permit;
  private Boolean approve;
  private Boolean smartContractSettlement;
  private String settlementContractAddress;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
