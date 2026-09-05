package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 自有地址工厂策略。 */
@Slf4j
@Component
public class AddressFactoryDerivedAddressStrategy implements DerivedAddressStrategy {

  private final CryptoPaymentProperties properties;

  public AddressFactoryDerivedAddressStrategy(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public DerivedAddressProvisionerMode mode() {
    return DerivedAddressProvisionerMode.ADDRESS_FACTORY;
  }

  @Override
  public List<String> createAddresses(String chain, String token, int count, String orderNo) {
    CryptoPaymentProperties.DerivedAddress.AddressFactory config = properties.getDerivedAddress().getAddressFactory();
    List<String> addresses = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      String seed = config.getNamespace() + ":" + config.getSeedPrefix() + ":" + chain + ":" + token + ":" + orderNo + ":" + i;
      addresses.add(hashToAddress(seed));
    }
    log.info("Generated factory derived addresses. chain={}, token={}, count={}, orderNo={}",
        chain, token, addresses.size(), orderNo);
    return addresses;
  }

  private String hashToAddress(String seed) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(seed.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder("0x");
      for (int i = 12; i < 32; i++) {
        builder.append(String.format("%02x", hash[i]));
      }
      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build factory address", ex);
    }
  }
}
