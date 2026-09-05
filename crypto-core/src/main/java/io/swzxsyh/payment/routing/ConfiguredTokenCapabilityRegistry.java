package io.swzxsyh.payment.routing;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(10)
public class ConfiguredTokenCapabilityRegistry implements TokenCapabilityRegistry {

  private final CryptoPaymentProperties properties;

  public ConfiguredTokenCapabilityRegistry(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public Optional<TokenCapabilityProfile> find(TokenRouteRequest request) {
    if (request == null) {
      return Optional.empty();
    }

    return properties.getTokenProfiles().stream()
        .filter(profile -> matches(profile, request))
        .findFirst()
        .map(profile -> new TokenCapabilityProfile(
            profile.getChain(),
            profile.getToken(),
            profile.getTokenAddress(),
            profile.getDecimals(),
            profile.isTransferWithAuthorization(),
            profile.isPermit(),
            profile.isApprove(),
            profile.isSmartContractSettlement(),
            profile.getSettlementContractAddress()
        ));
  }

  private boolean matches(CryptoPaymentProperties.TokenProfile profile, TokenRouteRequest request) {
    if (profile == null) {
      return false;
    }
    boolean chainMatches = !StringUtils.hasText(request.chain())
        || request.chain().equalsIgnoreCase(profile.getChain());
    boolean tokenMatches = !StringUtils.hasText(request.token())
        || request.token().equalsIgnoreCase(profile.getToken());
    boolean addressMatches = !StringUtils.hasText(request.tokenAddress())
        || request.tokenAddress().equalsIgnoreCase(profile.getTokenAddress());
    return chainMatches && tokenMatches && addressMatches;
  }
}
