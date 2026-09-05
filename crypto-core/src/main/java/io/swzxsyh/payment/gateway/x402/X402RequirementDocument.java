package io.swzxsyh.payment.gateway.x402;

import java.util.List;

/** X402 需求说明文档。 */
public record X402RequirementDocument(
    String serviceName,
    String facilitatorName,
    boolean enabled,
    boolean apiKeyRequired,
    List<String> requiredHeaders,
    List<String> supportedSchemes,
    List<String> supportedFlows
) {
}
