package io.swzxsyh.payment.gateway.x402;

import java.time.LocalDateTime;
import java.util.List;

/** X402 发现文档。 */
public record X402DiscoveryDocument(
    String serviceName,
    String facilitatorName,
    String publicBaseUrl,
    boolean enabled,
    boolean apiKeyRequired,
    LocalDateTime generatedAt,
    List<String> supportedSchemes,
    List<String> supportedFlows,
    List<X402ResourceDescriptor> resources,
    List<X402ClientProfile> clients
) {
}
