package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 按配置路由到具体派生地址策略。 */
@Slf4j
@Component
public class ConfiguredDerivedAddressProvisioner implements DerivedAddressProvisioner {

  private final CryptoPaymentProperties properties;
  private final Map<DerivedAddressProvisionerMode, DerivedAddressStrategy> strategies =
      new EnumMap<>(DerivedAddressProvisionerMode.class);

  public ConfiguredDerivedAddressProvisioner(
      CryptoPaymentProperties properties,
      List<DerivedAddressStrategy> strategyList) {
    this.properties = properties;
    for (DerivedAddressStrategy strategy : strategyList) {
      strategies.put(strategy.mode(), strategy);
    }
  }

  @Override
  public List<String> createAddresses(String chain, String token, int count, String orderNo) {
    if (!properties.getDerivedAddress().isEnabled()) {
      throw new IllegalStateException("Derived address is disabled");
    }
    DerivedAddressProvisionerMode selectedMode = resolveMode(chain);
    DerivedAddressStrategy strategy = strategies.get(selectedMode);
    if (strategy == null) {
      throw new IllegalStateException("No derived address strategy configured for mode: " + selectedMode);
    }

    log.info("Using derived address strategy. mode={}, chain={}, token={}, count={}, orderNo={}",
        selectedMode, chain, token, count, orderNo);
    return strategy.createAddresses(chain, token, count, orderNo);
  }

  private DerivedAddressProvisionerMode resolveMode(String chain) {
    String configuredMode = properties.getDerivedAddress().getMode();
    ChainFamily family = ChainFamilyResolver.resolve(chain);
    if (!StringUtils.hasText(configuredMode)) {
      if (family == ChainFamily.SOLANA) {
        return DerivedAddressProvisionerMode.SOLANA_HD;
      }
      return DerivedAddressProvisionerMode.ADDRESS_FACTORY;
    }
    try {
      DerivedAddressProvisionerMode mode =
          DerivedAddressProvisionerMode.valueOf(configuredMode.trim().toUpperCase());
      if (mode == DerivedAddressProvisionerMode.HD && family == ChainFamily.SOLANA) {
        return DerivedAddressProvisionerMode.SOLANA_HD;
      }
      return mode;
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unsupported derived address mode: " + configuredMode, ex);
    }
  }
}
