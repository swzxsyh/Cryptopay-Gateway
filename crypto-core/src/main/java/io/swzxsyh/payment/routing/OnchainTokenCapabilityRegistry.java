package io.swzxsyh.payment.routing;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.detector.TokenCapabilityProbe;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Order(20)
public class OnchainTokenCapabilityRegistry implements TokenCapabilityRegistry {

  private final CryptoPaymentProperties properties;
  private final TokenCapabilityProbe tokenCapabilityProbe;

  public OnchainTokenCapabilityRegistry(
      CryptoPaymentProperties properties,
      TokenCapabilityProbe tokenCapabilityProbe) {
    this.properties = properties;
    this.tokenCapabilityProbe = tokenCapabilityProbe;
  }

  @Override
  public Optional<TokenCapabilityProfile> find(TokenRouteRequest request) {
    if (request == null || !StringUtils.hasText(request.chain()) || !StringUtils.hasText(request.tokenAddress())) {
      return Optional.empty();
    }

    boolean contractExists = tokenCapabilityProbe.tokenContractExists(request.chain(), request.tokenAddress());
    if (!contractExists) {
      log.warn("Token contract does not exist or cannot be probed. chain={}, tokenAddress={}",
          request.chain(), request.tokenAddress());
      return Optional.empty();
    }

    boolean transferWithAuthorization = tokenCapabilityProbe.supportsTransferWithAuthorization(
        request.chain(), request.tokenAddress(), request.walletAddress());
    boolean permit = tokenCapabilityProbe.supportsPermit(
        request.chain(), request.tokenAddress(), request.walletAddress());
    boolean approve = tokenCapabilityProbe.supportsApprove(request.chain(), request.tokenAddress());

    log.info("Resolved token capability by on-chain probe. chain={}, token={}, tokenAddress={}, eip3009={}, permit={}, approve={}",
        request.chain(), request.token(), request.tokenAddress(), transferWithAuthorization, permit, approve);

    return Optional.of(new TokenCapabilityProfile(
        request.chain(),
        request.token(),
        request.tokenAddress(),
        6,
        transferWithAuthorization,
        permit,
        approve,
        true,
        properties.getContract().getAddress()
    ));
  }
}
