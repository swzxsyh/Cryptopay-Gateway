package io.swzxsyh.payment.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 智能合约分账计划，承载分账执行所需的全部信息。 */
public record ContractSettlementPlan(
    String cryptoOrderNo,
    String chain,
    String token,
    String contractAddress,
    BigDecimal amount,
    LocalDateTime expireTime,
    List<SettlementSplit> splits,
    String signPayload,
    String signature,
    String contractCallData
) {
}
