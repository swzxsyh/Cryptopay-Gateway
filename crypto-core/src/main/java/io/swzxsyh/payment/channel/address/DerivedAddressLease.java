package io.swzxsyh.payment.channel.address;

import java.time.LocalDateTime;

public record DerivedAddressLease(
    String poolKey,
    String leaseId,
    String address,
    String orderNo,
    boolean autoCreated,
    LocalDateTime leasedAt,
    LocalDateTime expireAt
) {
}
