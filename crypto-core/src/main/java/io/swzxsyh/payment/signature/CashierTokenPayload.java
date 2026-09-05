package io.swzxsyh.payment.signature;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record CashierTokenPayload(
    String tokenVersion,
    String keyAlias,
    String bizType,
    String cryptoOrderNo,
    String subscriptionOrderNo,
    String merchantId,
    String merchantOrderNo,
    BigDecimal amount,
    String currency,
    String chain,
    String token,
    LocalDateTime issuedAt,
    LocalDateTime expireAt,
    Map<String, Object> attributes
) {
}
