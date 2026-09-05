package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 商户可用支付链路和费率配置，按商户 + 链 + 币种维度控制收银台可选项。 */
@Data
@TableName("merchant_payment_channel_config")
public class MerchantPaymentChannelConfig {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String merchantId;
  private String chainCode;
  private String tokenSymbol;
  private Boolean enabled;
  private BigDecimal transactionFeeRate;
  private BigDecimal minimumFee;
  private BigDecimal fixedFee;
  private BigDecimal gatewayFee;
  private BigDecimal taxRate;
  private String feeSettlementMode;
  private BigDecimal minOrderAmount;
  private BigDecimal maxOrderAmount;
  private String remark;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
