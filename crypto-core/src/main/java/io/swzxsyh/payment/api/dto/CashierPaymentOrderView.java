package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.routing.WalletAccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 用户收银台视图，只暴露页面支付所需字段，不返回回调地址、费率快照和内部路由原因。 */
public record CashierPaymentOrderView(
    String cryptoOrderNo,
    String merchantOrderNo,
    BigDecimal amount,
    String currency,
    OrderStatus status,
    PaymentMethod paymentMethod,
    String walletAddress,
    WalletAccountType walletAccountType,
    String paymentAddress,
    String contractAddress,
    String contractCallData,
    String chain,
    String token,
    String tokenAddress,
    String paymentTxHash,
    BigDecimal realAmount,
    LocalDateTime paidAt,
    boolean latePayment,
    LocalDateTime latePaymentAt,
    PendingChainTransactionView pendingTransaction,
    String returnUrl,
    LocalDateTime expireTime,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public static CashierPaymentOrderView from(PaymentOrder order) {
    return from(order, null);
  }

  public static CashierPaymentOrderView from(
      PaymentOrder order, PendingChainTransactionView pendingTransaction) {
    return new CashierPaymentOrderView(
        order.getCryptoOrderNo(),
        order.getMerchantOrderNo(),
        order.getAmount(),
        order.getCurrency(),
        order.getStatus(),
        order.getPaymentMethod(),
        order.getWalletAddress(),
        parseWalletAccountType(order.getWalletAccountType()),
        order.getPaymentAddress(),
        order.getContractAddress(),
        order.getContractCallData(),
        order.getChain(),
        order.getToken(),
        order.getTokenAddress(),
        order.getPaymentTxHash(),
        order.getRealAmount(),
        order.getPaidAt(),
        order.isLatePayment(),
        order.getLatePaymentAt(),
        pendingTransaction,
        order.getReturnUrl(),
        order.getExpireTime(),
        order.getCreatedAt(),
        order.getUpdatedAt());
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
