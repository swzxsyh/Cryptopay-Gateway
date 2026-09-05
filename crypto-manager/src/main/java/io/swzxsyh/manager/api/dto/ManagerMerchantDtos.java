package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.MerchantInfo;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAccount;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import io.swzxsyh.payment.persistence.entity.MerchantWithdrawRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 商户主数据管理 DTO。 */
public final class ManagerMerchantDtos {

  private ManagerMerchantDtos() {}

  /** 商户主数据保存请求；签名公钥等商户侧安全材料不在 manager 端维护。 */
  public record MerchantSaveRequest(
      Long id,
      String merchantId,
      String merchantName,
      String status,
      String contactEmail,
      String contactPhone,
      String defaultNotifyUrl,
      String defaultReturnUrl,
      String defaultWithdrawChain,
      String defaultWithdrawAddress,
      String remark) {}

  /** 商户列表与详情响应。 */
  public record MerchantView(
      Long id,
      String merchantId,
      String merchantName,
      String status,
      String contactEmail,
      String contactPhone,
      String defaultNotifyUrl,
      String defaultReturnUrl,
      String defaultWithdrawChain,
      String defaultWithdrawAddress,
      String remark,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static MerchantView from(MerchantInfo merchant) {
      return new MerchantView(
          merchant.getId(),
          merchant.getMerchantId(),
          merchant.getMerchantName(),
          merchant.getStatus(),
          merchant.getContactEmail(),
          merchant.getContactPhone(),
          merchant.getDefaultNotifyUrl(),
          merchant.getDefaultReturnUrl(),
          merchant.getDefaultWithdrawChain(),
          merchant.getDefaultWithdrawAddress(),
          merchant.getRemark(),
          merchant.getCreatedAt(),
          merchant.getUpdatedAt());
    }
  }

  /** 商户余额账户视图。 */
  public record BalanceAccountView(
      Long id,
      String merchantId,
      String balanceToken,
      BigDecimal availableBalance,
      BigDecimal frozenBalance,
      BigDecimal totalIncome,
      BigDecimal totalWithdrawn,
      String status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static BalanceAccountView from(MerchantBalanceAccount account) {
      return new BalanceAccountView(
          account.getId(),
          account.getMerchantId(),
          account.getBalanceToken(),
          account.getAvailableBalance(),
          account.getFrozenBalance(),
          account.getTotalIncome(),
          account.getTotalWithdrawn(),
          account.getStatus(),
          account.getCreatedAt(),
          account.getUpdatedAt());
    }
  }

  /** 管理端调账请求；只允许修正商户资金余额，不代表商户提现。 */
  public record BalanceAdjustRequest(
      String merchantId,
      String token,
      BigDecimal amount,
      String direction,
      String reason,
      String operatorNote) {}

  /** 商户余额调账结果。 */
  public record BalanceAdjustResponse(
      String approvalNo,
      String adjustNo,
      String merchantId,
      String token,
      String direction,
      BigDecimal amount,
      String status) {}

  /** 提现操作请求。 */
  public record WithdrawHandleRequest(
      String txHash,
      String operator,
      String operatorNote,
      String failureReason) {}

  /** 提现流水视图。 */
  public record WithdrawRecordView(
      Long id,
      String withdrawNo,
      String merchantId,
      String token,
      BigDecimal amount,
      String chain,
      String withdrawAddress,
      String txHash,
      String status,
      String operator,
      String operatorNote,
      String failureReason,
      LocalDateTime requestedAt,
      LocalDateTime reviewedAt,
      LocalDateTime submittedAt,
      LocalDateTime completedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static WithdrawRecordView from(MerchantWithdrawRecord record) {
      return new WithdrawRecordView(
          record.getId(),
          record.getWithdrawNo(),
          record.getMerchantId(),
          record.getToken(),
          record.getAmount(),
          record.getChain(),
          record.getWithdrawAddress(),
          record.getTxHash(),
          record.getStatus(),
          record.getOperator(),
          record.getOperatorNote(),
          record.getFailureReason(),
          record.getRequestedAt(),
          record.getReviewedAt(),
          record.getSubmittedAt(),
          record.getCompletedAt(),
          record.getCreatedAt(),
          record.getUpdatedAt());
    }
  }

  /** 商户支付通道费率保存请求。 */
  public record PaymentChannelSaveRequest(
      Long id,
      String merchantId,
      String chainCode,
      String tokenSymbol,
      Boolean enabled,
      BigDecimal transactionFeeRate,
      BigDecimal minimumFee,
      BigDecimal fixedFee,
      BigDecimal gatewayFee,
      BigDecimal taxRate,
      BigDecimal minOrderAmount,
      BigDecimal maxOrderAmount,
      String remark) {}

  /** 批量开通商户支付通道请求，适配前端勾选多个链币组合后一次性提交。 */
  public record PaymentChannelBatchSaveRequest(
      String merchantId,
      List<PaymentChannelSelection> selections,
      Boolean enabled,
      BigDecimal transactionFeeRate,
      BigDecimal minimumFee,
      BigDecimal fixedFee,
      BigDecimal gatewayFee,
      BigDecimal taxRate,
      BigDecimal minOrderAmount,
      BigDecimal maxOrderAmount,
      String remark) {}

  /** 商户支付通道勾选项。 */
  public record PaymentChannelSelection(String chainCode, String tokenSymbol) {}

  /** 商户支付通道费率视图。 */
  public record PaymentChannelView(
      Long id,
      String merchantId,
      String chainCode,
      String tokenSymbol,
      Boolean enabled,
      BigDecimal transactionFeeRate,
      BigDecimal minimumFee,
      BigDecimal fixedFee,
      BigDecimal gatewayFee,
      BigDecimal taxRate,
      BigDecimal minOrderAmount,
      BigDecimal maxOrderAmount,
      String remark,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static PaymentChannelView from(MerchantPaymentChannelConfig config) {
      return new PaymentChannelView(
          config.getId(),
          config.getMerchantId(),
          config.getChainCode(),
          config.getTokenSymbol(),
          config.getEnabled(),
          config.getTransactionFeeRate(),
          config.getMinimumFee(),
          config.getFixedFee(),
          config.getGatewayFee(),
          config.getTaxRate(),
          config.getMinOrderAmount(),
          config.getMaxOrderAmount(),
          config.getRemark(),
          config.getCreatedAt(),
          config.getUpdatedAt());
    }
  }
}
