package io.swzxsyh.payment.gateway.x402;

import java.util.List;

/** X402 客户端画像。 */
public record X402ClientProfile(
    String id,
    String title,
    String description,
    List<String> transportHints,
    List<String> supportedFlows
) {
}
