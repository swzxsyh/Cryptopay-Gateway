package io.swzxsyh.payment.settlement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ContractSettlementCommand(
    String cryptoOrderNo,
    String chain,
    String token,
    String tokenAddress,
    int tokenDecimals,
    String contractAddress,
    BigDecimal amount,
    LocalDateTime expireTime,
    List<SettlementRule> settlementRules) {}
