package io.swzxsyh.payment.accounting;

import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import java.math.BigDecimal;

/** 商户支付通道费率快照，写入订单后用于锁定历史计费口径。 */
public record MerchantSettlementFeeSnapshot(
    BigDecimal transactionFeeRate,
    BigDecimal minimumFee,
    BigDecimal fixedFee,
    BigDecimal gatewayFee,
    BigDecimal taxRate,
    String feeSettlementMode) {

  public static MerchantSettlementFeeSnapshot zero() {
    return new MerchantSettlementFeeSnapshot(
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null);
  }

  public static MerchantSettlementFeeSnapshot from(MerchantPaymentChannelConfig config) {
    if (config == null) {
      return zero();
    }
    return new MerchantSettlementFeeSnapshot(
        value(config.getTransactionFeeRate()),
        value(config.getMinimumFee()),
        value(config.getFixedFee()),
        value(config.getGatewayFee()),
        value(config.getTaxRate()),
        config.getFeeSettlementMode());
  }

  private static BigDecimal value(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
