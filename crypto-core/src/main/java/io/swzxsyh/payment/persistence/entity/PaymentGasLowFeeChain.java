package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Gas 不足时建议切换的低费链。 */
@Data
@TableName("payment_gas_low_fee_chain")
public class PaymentGasLowFeeChain {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String chainCode;
  private Integer sortNo;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
