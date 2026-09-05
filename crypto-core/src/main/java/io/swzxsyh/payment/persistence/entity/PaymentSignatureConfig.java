package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 请求验签和回调加签的开关与头名配置。 */
@Data
@TableName("payment_signature_config")
public class PaymentSignatureConfig {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String configScope;
  private Boolean signatureEnabled;
  private Boolean verifyInboundEnabled;
  private Boolean signOutboundEnabled;
  private String algorithm;
  private String merchantIdHeader;
  private String timestampHeader;
  private String nonceHeader;
  private String signatureHeader;
  private String keyVersionHeader;
  private Long allowedClockSkewSeconds;
  private Long replayTtlSeconds;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
