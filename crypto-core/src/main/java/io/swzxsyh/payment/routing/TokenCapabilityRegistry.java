package io.swzxsyh.payment.routing;

import java.util.Optional;

public interface TokenCapabilityRegistry {

  Optional<TokenCapabilityProfile> find(TokenRouteRequest request);
}
