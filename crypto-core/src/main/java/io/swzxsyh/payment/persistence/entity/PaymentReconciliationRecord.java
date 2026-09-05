package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** 支付对账结果记录，用于保存订单、链上流水、回调和分账记录之间的差异。 */
@Data
@TableName("payment_reconciliation_record")
public class PaymentReconciliationRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 对账唯一键，避免同一业务对象重复生成对账记录。 */
  private String reconcileKey;
  /** 对账对象类型：ORDER / RAW_CHAIN_LOG。 */
  private String bizType;
  /** 对账对象主键，如平台订单号或 chain:txHash:logIndex。 */
  private String bizKey;
  /** 商户号。 */
  private String merchantId;
  /** 平台支付单号。 */
  private String cryptoOrderNo;
  /** 商户订单号。 */
  private String merchantOrderNo;
  /** 链编码。 */
  private String chain;
  /** 币种。 */
  private String token;
  /** 代币合约地址。 */
  private String tokenAddress;
  /** 交易哈希。 */
  private String txHash;
  /** 日志序号。 */
  private Long logIndex;
  /** 订单应收金额。 */
  private BigDecimal expectedAmount;
  /** 链上实际到账金额。 */
  private BigDecimal realAmount;
  /** 差额：realAmount - expectedAmount。 */
  private BigDecimal diffAmount;
  /** 订单状态快照。 */
  private String orderStatus;
  /** 原始链上流水状态快照。 */
  private String rawLogStatus;
  /** 回调状态快照。 */
  private String callbackStatus;
  /** 合约分账状态快照。 */
  private String settlementStatus;
  /** 对账状态：MATCHED / WARNING / MISMATCH / MANUAL_CONFIRMED。 */
  private String reconcileStatus;
  /** 问题类型，如 AMOUNT_MISMATCH / UNMATCHED_CHAIN_LOG / CALLBACK_NOT_DELIVERED。 */
  private String issueType;
  /** 问题说明。 */
  private String issueReason;
  /** 运营处理人。 */
  private String operator;
  /** 运营处理备注。 */
  private String operatorNote;
  /** 人工确认时间。 */
  private LocalDateTime manualConfirmedAt;
  /** 最近对账时间。 */
  private LocalDateTime reconciledAt;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;
}
