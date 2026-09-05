package io.swzxsyh.payment.routing;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Token 能力解析器。 */
@Slf4j
@Service
public class TokenCapabilityResolver {

  private final List<TokenCapabilityRegistry> registries;

  public TokenCapabilityResolver(List<TokenCapabilityRegistry> registries) {
    this.registries = registries;
  }

  public Optional<TokenCapabilityProfile> resolve(TokenRouteRequest request) {
    for (TokenCapabilityRegistry registry : registries) {
      Optional<TokenCapabilityProfile> profile = registry.find(request);
      if (profile.isPresent()) {
        log.debug("Resolved token capability by registry={}", registry.getClass().getSimpleName());
        return profile;
      }
    }
    return Optional.empty();
  }
}
