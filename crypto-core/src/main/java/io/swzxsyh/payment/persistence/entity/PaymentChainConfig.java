package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 支付支持的链配置。 */
@Data
@TableName("payment_chain_config")
public class PaymentChainConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String chainCode;
  private String rpcUrl;
  private String wsUrl;
  private Integer confirmationDepth;
  private Boolean sponsorEnabled;
  private String sponsorProvider;
  private String relayerAddress;
  private Long sponsorGasLimit;
  private Boolean enabled;
  private Integer sortNo;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
