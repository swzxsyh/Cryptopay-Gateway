package io.swzxsyh.payment.gateway.x402;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.routing.TokenRoutePlan;
import io.swzxsyh.payment.routing.TokenRoutePlanner;
import io.swzxsyh.payment.routing.TokenRouteRequest;
import io.swzxsyh.payment.routing.TokenRouteType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

/** X402 协议能力服务。 */
@Service
public class X402ProtocolService {

  private final CryptoPaymentProperties properties;
  private final X402Facilitator facilitator;
  private final TokenRoutePlanner tokenRoutePlanner;

  public X402ProtocolService(
      CryptoPaymentProperties properties,
      X402Facilitator facilitator,
      TokenRoutePlanner tokenRoutePlanner) {
    this.properties = properties;
    this.facilitator = facilitator;
    this.tokenRoutePlanner = tokenRoutePlanner;
  }

  public X402DiscoveryDocument discovery() {
    X402FacilitatorDescriptor descriptor = facilitator.describe();
    return new X402DiscoveryDocument(
        "CryptoPay Gateway",
        descriptor.name(),
        properties.getDiscovery().getPublicBaseUrl(),
        properties.getGateway().isEnabled(),
        descriptor.apiKeyRequired(),
        LocalDateTime.now(),
        descriptor.supportedSchemes(),
        descriptor.supportedRoutes(),
        resources(),
        clients()
    );
  }

  public X402SupportDocument support() {
    X402FacilitatorDescriptor descriptor = facilitator.describe();
    return new X402SupportDocument(
            "CryptoPay Gateway",
        descriptor.name(),
        properties.getGateway().isEnabled(),
        descriptor.apiKeyRequired(),
        properties.getGateway().isPublicResourcesEnabled(),
        descriptor.supportedSchemes(),
        descriptor.supportedRoutes(),
        resources(),
        clients()
    );
  }

  public X402RequirementDocument requirements() {
    X402FacilitatorDescriptor descriptor = facilitator.describe();
    return new X402RequirementDocument(
            "CryptoPay Gateway",
        descriptor.name(),
        properties.getGateway().isEnabled(),
        descriptor.apiKeyRequired(),
        List.of(
            X402AccessInterceptor.HEADER_API_KEY,
            "Content-Type"
        ),
        descriptor.supportedSchemes(),
        descriptor.supportedRoutes()
    );
  }

  public Eip3009PreviewResponse previewEip3009(Eip3009PreviewRequest request) {
    TokenRoutePlan routePlan = tokenRoutePlanner.plan(new TokenRouteRequest(
        request.chain(),
        request.token(),
        request.tokenAddress(),
        request.walletAddress(),
        request.walletAccountType()
    ));
    boolean supported = routePlan.routeType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION;
    Eip3009AuthorizationTemplate authorization = supported
        ? new Eip3009AuthorizationTemplate(
            request.chain(),
            request.token(),
            request.tokenAddress(),
            request.walletAddress(),
            request.recipientAddress(),
            request.amount(),
            6,
            null,
            null,
            null,
            "EIP-3009",
            "transferWithAuthorization",
            routePlan.payload())
        : null;
    return new Eip3009PreviewResponse(
        supported,
        routePlan.routeType().name(),
        routePlan.routeReason(),
        true,
        request.token(),
        request.tokenAddress(),
        authorization
    );
  }

  private List<X402ResourceDescriptor> resources() {
    return List.of(
        new X402ResourceDescriptor(
            "x402-discovery",
            "Discovery",
            "/api/crypto/x402/discovery",
            "GET",
            false,
            "Self-describing capability document."
        ),
        new X402ResourceDescriptor(
            "x402-support",
            "Support",
            "/api/crypto/x402/support",
            "GET",
            false,
            "Human-readable support and capability summary."
        ),
        new X402ResourceDescriptor(
            "x402-requirements",
            "Requirements",
            "/api/crypto/x402/requirements",
            "GET",
            true,
            "Protocol requirements for authenticated clients."
        ),
        new X402ResourceDescriptor(
            "x402-orders",
            "Create Order",
            "/api/crypto/x402/orders",
            "POST",
            true,
            "Create a gateway order through the facilitator."
        ),
        new X402ResourceDescriptor(
            "x402-eip3009-preview",
            "EIP-3009 Preview",
            "/api/crypto/x402/eip3009/preview",
            "POST",
            true,
            "Standardized transferWithAuthorization preview."
        )
    );
  }

  private List<X402ClientProfile> clients() {
    return List.of(
        new X402ClientProfile(
            "merchant-server",
            "Merchant Server",
            "Server-to-server caller for order creation and protocol negotiation.",
            List.of("HTTP/JSON", "X-API-Key"),
            List.of("requirements", "order-create", "poll-discovery")
        ),
        new X402ClientProfile(
            "wallet-agent",
            "Wallet Agent",
            "Client that prepares wallet-side EIP-3009 authorization flows.",
            List.of("HTTP/JSON", "browser-wallet"),
            List.of("eip3009-preview", "support", "discovery")
        )
    );
  }
}
