package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 智能合约收款和分账入口配置。 */
@Data
@TableName("payment_contract_config")
public class PaymentContractConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean contractEnabled;
  private String contractAddress;
  private Boolean create2Enabled;
  private Boolean create2HostedWalletOnly;
  private String create2FactoryAddress;
  private String create2InitCodeHash;
  private String create2SaltPrefix;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
