package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** Gas 代付尝试记录，用于防刷、审计和后续商户/客户维度统计。 */
@Data
@TableName("gas_sponsor_attempt_record")
public class GasSponsorAttemptRecord {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String cryptoOrderNo;
  private String merchantId;
  private String chain;
  private String token;
  private String tokenAddress;
  private String walletAddress;
  private String ipAddress;
  private String cashierTokenHash;
  private String providerId;
  private String requestType;
  private String status;
  private String txHash;
  private String rejectReason;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
