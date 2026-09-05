package io.swzxsyh.payment.accounting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * 商户结算费用计算器。
 *
 * <p>后续新增阶梯费率、封顶手续费、活动减免、网络成本分摊时，都应该在这里扩展，
 * 避免订单、订阅、余额流水各自实现一套计算逻辑。
 */
@Component
public class MerchantSettlementFeeCalculator {

  private static final int MONEY_SCALE = 18;

  /** 根据真实到账金额和订单费率快照计算商户净入账。 */
  public MerchantSettlementFeeResult calculate(
      BigDecimal realAmount, MerchantSettlementFeeSnapshot snapshot) {
    BigDecimal grossAmount = value(realAmount);
    MerchantSettlementFeeSnapshot safeSnapshot =
        snapshot == null ? MerchantSettlementFeeSnapshot.zero() : snapshot;
    BigDecimal rateFee = grossAmount.multiply(value(safeSnapshot.transactionFeeRate()));
    BigDecimal transactionFee = max(rateFee, value(safeSnapshot.minimumFee()));
    BigDecimal fixedFee = value(safeSnapshot.fixedFee());
    BigDecimal gatewayFee = value(safeSnapshot.gatewayFee());
    BigDecimal taxFee = grossAmount.multiply(value(safeSnapshot.taxRate()));
    BigDecimal totalFee = transactionFee.add(fixedFee).add(gatewayFee).add(taxFee);
    BigDecimal settlementAmount = grossAmount.subtract(totalFee);
    if (settlementAmount.signum() < 0) {
      settlementAmount = BigDecimal.ZERO;
    }
    return new MerchantSettlementFeeResult(
        scale(grossAmount),
        scale(transactionFee),
        scale(fixedFee),
        scale(gatewayFee),
        scale(taxFee),
        scale(totalFee),
        scale(settlementAmount));
  }

  private BigDecimal max(BigDecimal left, BigDecimal right) {
    return left.compareTo(right) >= 0 ? left : right;
  }

  private BigDecimal value(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private BigDecimal scale(BigDecimal value) {
    return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
  }
}
