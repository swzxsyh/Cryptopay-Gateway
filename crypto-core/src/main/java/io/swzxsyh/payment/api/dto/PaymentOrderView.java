package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.routing.WalletAccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 面向前端展示的支付订单视图。 */
public record PaymentOrderView(
    String cryptoOrderNo,
    String merchantId,
    String merchantOrderNo,
    BigDecimal amount,
    String currency,
    String chain,
    String token,
    String tokenAddress,
    String walletAddress,
    WalletAccountType walletAccountType,
    PaymentMethod paymentMethod,
    String tokenRouteType,
    String routeReason,
    OrderStatus status,
    String paymentAddress,
    String contractAddress,
    String contractCallData,
    String gasPayerMode,
    String gasReason,
    java.math.BigDecimal gasEstimatedFeeWei,
    boolean gasCustomerBalanceSufficient,
    boolean gasPlatformBalanceSufficient,
    String gasFallbackSuggestion,
    String derivedAddressPoolKey,
    String derivedAddressLeaseId,
    BigDecimal transactionFeeRate,
    BigDecimal minimumFee,
    BigDecimal fixedFee,
    BigDecimal gatewayFee,
    BigDecimal taxRate,
    String feeSettlementMode,
    BigDecimal transactionFee,
    BigDecimal taxFee,
    BigDecimal totalFee,
    BigDecimal settlementAmount,
    String paymentTxHash,
    BigDecimal realAmount,
    LocalDateTime paidAt,
    boolean latePayment,
    LocalDateTime latePaymentAt,
    PendingChainTransactionView pendingTransaction,
    String notifyUrl,
    String returnUrl,
    LocalDateTime expireTime,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public static PaymentOrderView from(PaymentOrder order) {
    return new PaymentOrderView(
        order.getCryptoOrderNo(),
        order.getMerchantId(),
        order.getMerchantOrderNo(),
        order.getAmount(),
        order.getCurrency(),
        order.getChain(),
        order.getToken(),
        order.getTokenAddress(),
        order.getWalletAddress(),
        parseWalletAccountType(order.getWalletAccountType()),
        order.getPaymentMethod(),
        order.getTokenRouteType(),
        order.getRouteReason(),
        order.getStatus(),
        order.getPaymentAddress(),
        order.getContractAddress(),
        order.getContractCallData(),
        order.getGasPayerMode(),
        order.getGasReason(),
        order.getGasEstimatedFeeWei(),
        order.isGasCustomerBalanceSufficient(),
        order.isGasPlatformBalanceSufficient(),
        order.getGasFallbackSuggestion(),
        order.getDerivedAddressPoolKey(),
        order.getDerivedAddressLeaseId(),
        order.getTransactionFeeRate(),
        order.getMinimumFee(),
        order.getFixedFee(),
        order.getGatewayFee(),
        order.getTaxRate(),
        order.getFeeSettlementMode(),
        order.getTransactionFee(),
        order.getTaxFee(),
        order.getTotalFee(),
        order.getSettlementAmount(),
        order.getPaymentTxHash(),
        order.getRealAmount(),
        order.getPaidAt(),
        order.isLatePayment(),
        order.getLatePaymentAt(),
        null,
        order.getNotifyUrl(),
        order.getReturnUrl(),
        order.getExpireTime(),
        order.getCreatedAt(),
        order.getUpdatedAt()
    );
  }

  public static PaymentOrderView from(PaymentOrder order, PendingChainTransactionView pendingTransaction) {
    PaymentOrderView base = from(order);
    return new PaymentOrderView(
        base.cryptoOrderNo(),
        base.merchantId(),
        base.merchantOrderNo(),
        base.amount(),
        base.currency(),
        base.chain(),
        base.token(),
        base.tokenAddress(),
        base.walletAddress(),
        base.walletAccountType(),
        base.paymentMethod(),
        base.tokenRouteType(),
        base.routeReason(),
        base.status(),
        base.paymentAddress(),
        base.contractAddress(),
        base.contractCallData(),
        base.gasPayerMode(),
        base.gasReason(),
        base.gasEstimatedFeeWei(),
        base.gasCustomerBalanceSufficient(),
        base.gasPlatformBalanceSufficient(),
        base.gasFallbackSuggestion(),
        base.derivedAddressPoolKey(),
        base.derivedAddressLeaseId(),
        base.transactionFeeRate(),
        base.minimumFee(),
        base.fixedFee(),
        base.gatewayFee(),
        base.taxRate(),
        base.feeSettlementMode(),
        base.transactionFee(),
        base.taxFee(),
        base.totalFee(),
        base.settlementAmount(),
        base.paymentTxHash(),
        base.realAmount(),
        base.paidAt(),
        base.latePayment(),
        base.latePaymentAt(),
        pendingTransaction,
        base.notifyUrl(),
        base.returnUrl(),
        base.expireTime(),
        base.createdAt(),
        base.updatedAt());
  }

  private static WalletAccountType parseWalletAccountType(String value) {
    if (value == null || value.isBlank()) {
      return WalletAccountType.UNKNOWN;
    }
    try {
      return WalletAccountType.valueOf(value);
    } catch (IllegalArgumentException ex) {
      return WalletAccountType.UNKNOWN;
    }
  }
}
