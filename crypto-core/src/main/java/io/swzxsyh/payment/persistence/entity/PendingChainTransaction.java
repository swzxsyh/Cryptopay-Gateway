package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 未确认链上交易旁路表，用于收银台展示确认进度，最终认账仍走原链路。 */
@Data
@TableName("pending_chain_transaction")
public class PendingChainTransaction {
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String chain;
  private String txHash;
  private Long logIndex;
  private String source;
  private String token;
  private String tokenAddress;
  private String fromAddress;
  private String toAddress;
  private BigDecimal amount;
  private Long blockNumber;
  private LocalDateTime blockTimestamp;
  private Integer targetConfirmations;
  private Integer currentConfirmations;
  private String status;
  private String matchedCryptoOrderNo;
  private LocalDateTime observedAt;
  private LocalDateTime lastDispatchedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
