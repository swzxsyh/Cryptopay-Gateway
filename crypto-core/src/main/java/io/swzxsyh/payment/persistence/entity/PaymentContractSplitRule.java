package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 智能合约分账比例规则。 */
@Data
@TableName("payment_contract_split_rule")
public class PaymentContractSplitRule {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private String roleCode;
  private String receiverAddress;
  private Integer basisPoints;
  private Integer sortNo;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
