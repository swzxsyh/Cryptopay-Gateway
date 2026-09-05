package io.swzxsyh.payment.settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 单条分账结果，包含角色、收款方、比例和实际金额。 */
public record SettlementSplit(
    String role,
    String receiver,
    int basisPoints,
    BigDecimal amount
) {

  /** 根据总金额和分账比例计算单条分账金额。 */
  public static SettlementSplit of(String role, String receiver, int basisPoints, BigDecimal totalAmount) {
    if (basisPoints <= 0) {
      throw new IllegalArgumentException("basisPoints must be greater than zero");
    }
    BigDecimal amount = totalAmount
        .multiply(BigDecimal.valueOf(basisPoints))
        .divide(BigDecimal.valueOf(10000), 6, RoundingMode.DOWN);
    return new SettlementSplit(role, receiver, basisPoints, amount);
  }
}
