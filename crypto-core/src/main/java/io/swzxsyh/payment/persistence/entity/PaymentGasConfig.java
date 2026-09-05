package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Gas 代付和预检策略配置。 */
@Data
@TableName("payment_gas_config")
public class PaymentGasConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean gasEnabled;
  private Boolean hostedWalletPreferPlatform;
  private Boolean hostedWalletSponsorEnabled;
  private Boolean evmSponsorEnabled;
  private String evmRelayerPrivateKeySourceType;
  private String evmRelayerPrivateKeyEnv;
  private String evmRelayerPrivateKeyKmsKeyId;
  private Boolean sponsorProtectionEnabled;
  private Integer sponsorCounterTtlSeconds;
  private Integer sponsorOrderMaxAttempts;
  private Integer sponsorWalletMaxAttempts;
  private Integer sponsorIpMaxAttempts;
  private Integer sponsorCashierTokenMaxAttempts;
  private Integer sponsorOrderLockLeaseSeconds;
  private Boolean customerBalancePrecheckEnabled;
  private Integer platformSponsoredGasLimit;
  private Integer customerGasLimit;
  private Integer freeTransferGasLimit;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
