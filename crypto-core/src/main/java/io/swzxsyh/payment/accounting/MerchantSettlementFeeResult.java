package io.swzxsyh.payment.accounting;

import java.math.BigDecimal;

/** 商户订单结算费用计算结果。 */
public record MerchantSettlementFeeResult(
    BigDecimal grossAmount,
    BigDecimal transactionFee,
    BigDecimal fixedFee,
    BigDecimal gatewayFee,
    BigDecimal taxFee,
    BigDecimal totalFee,
    BigDecimal settlementAmount) {}
