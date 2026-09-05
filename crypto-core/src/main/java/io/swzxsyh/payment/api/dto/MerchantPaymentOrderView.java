package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商户查单视图，只返回商户对账和支付结果需要的字段。 */
public record MerchantPaymentOrderView(
    String cryptoOrderNo,
    String merchantId,
    String merchantOrderNo,
    BigDecimal amount,
    String currency,
    String chain,
    String token,
    String tokenAddress,
    PaymentMethod paymentMethod,
    OrderStatus status,
    String paymentTxHash,
    BigDecimal realAmount,
    LocalDateTime paidAt,
    boolean latePayment,
    LocalDateTime latePaymentAt,
    String returnUrl,
    LocalDateTime expireTime,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public static MerchantPaymentOrderView from(PaymentOrder order) {
    return new MerchantPaymentOrderView(
        order.getCryptoOrderNo(),
        order.getMerchantId(),
        order.getMerchantOrderNo(),
        order.getAmount(),
        order.getCurrency(),
        order.getChain(),
        order.getToken(),
        order.getTokenAddress(),
        order.getPaymentMethod(),
        order.getStatus(),
        order.getPaymentTxHash(),
        order.getRealAmount(),
        order.getPaidAt(),
        order.isLatePayment(),
        order.getLatePaymentAt(),
        order.getReturnUrl(),
        order.getExpireTime(),
        order.getCreatedAt(),
        order.getUpdatedAt());
  }
}
