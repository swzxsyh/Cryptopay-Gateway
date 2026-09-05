package io.swzxsyh.payment.gateway.x402;

/** X402 资源描述。 */
public record X402ResourceDescriptor(
    String id,
    String title,
    String path,
    String method,
    boolean protectedResource,
    String description
) {
}
