package io.swzxsyh.payment.gateway.x402;

import java.util.List;

/** X402 支持能力文档。 */
public record X402SupportDocument(
    String serviceName,
    String facilitatorName,
    boolean enabled,
    boolean apiKeyRequired,
    boolean publicResourcesEnabled,
    List<String> supportedSchemes,
    List<String> supportedFlows,
    List<X402ResourceDescriptor> resources,
    List<X402ClientProfile> clients
) {
}
