package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 统一判断某条链是否允许平台代付，支持全局缺省和单链覆盖。 */
@Service
public class GasSponsorshipPolicyService {

  private final CryptoPaymentProperties properties;

  public GasSponsorshipPolicyService(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  public boolean isSponsorEnabled(String chain, String providerId) {
    if (!properties.getGas().isEnabled()) {
      return false;
    }
    CryptoPaymentProperties.ChainProfile profile = findChain(chain);
    if (profile != null && Boolean.FALSE.equals(profile.getSponsorEnabled())) {
      return false;
    }
    if (profile != null && !providerAllowed(profile.getSponsorProvider(), providerId)) {
      return false;
    }
    if (profile != null && Boolean.TRUE.equals(profile.getSponsorEnabled())) {
      return true;
    }
    ChainFamily family = ChainFamilyResolver.resolve(chain);
    if (family == ChainFamily.SOLANA) {
      return properties.getSolana().isFeePayerEnabled();
    }
    if (family == ChainFamily.EVM) {
      return properties.getGas().isHostedWalletSponsorEnabled()
          && properties.getGas().isEvmSponsorEnabled();
    }
    return false;
  }

  public long sponsorGasLimit(String chain, long defaultLimit) {
    CryptoPaymentProperties.ChainProfile profile = findChain(chain);
    if (profile != null && profile.getSponsorGasLimit() != null && profile.getSponsorGasLimit() > 0) {
      return profile.getSponsorGasLimit();
    }
    return defaultLimit;
  }

  public String relayerAddress(String chain) {
    CryptoPaymentProperties.ChainProfile profile = findChain(chain);
    return profile == null ? null : profile.getRelayerAddress();
  }

  private boolean providerAllowed(String configuredProvider, String providerId) {
    if (!StringUtils.hasText(configuredProvider) || "AUTO".equalsIgnoreCase(configuredProvider)) {
      return true;
    }
    if ("NONE".equalsIgnoreCase(configuredProvider)) {
      return false;
    }
    return normalize(configuredProvider).equals(normalize(providerId));
  }

  private CryptoPaymentProperties.ChainProfile findChain(String chain) {
    if (!StringUtils.hasText(chain)) {
      return null;
    }
    return properties.getChainProfiles().stream()
        .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
        .findFirst()
        .orElse(null);
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().replace("-", "_").toUpperCase(Locale.ROOT);
  }
}
