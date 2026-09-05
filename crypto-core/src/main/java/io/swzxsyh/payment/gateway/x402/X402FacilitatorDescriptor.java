package io.swzxsyh.payment.gateway.x402;

import java.util.List;

/** X402 facilitator 描述。 */
public record X402FacilitatorDescriptor(
    String name,
    String version,
    boolean apiKeyRequired,
    boolean publicResourcesEnabled,
    List<String> supportedSchemes,
    List<String> supportedRoutes
) {
}
