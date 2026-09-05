package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

/** Keystore 批量生成策略。 */
@Slf4j
@Component
public class KeystoreDerivedAddressStrategy implements DerivedAddressStrategy {

  private final CryptoPaymentProperties properties;
  private final LocalKeystoreArtifactWriter keystoreWriter;
  private final SecretValueResolver secretValueResolver;

  public KeystoreDerivedAddressStrategy(
      CryptoPaymentProperties properties,
      LocalKeystoreArtifactWriter keystoreWriter,
      SecretValueResolver secretValueResolver) {
    this.properties = properties;
    this.keystoreWriter = keystoreWriter;
    this.secretValueResolver = secretValueResolver;
  }

  @Override
  public DerivedAddressProvisionerMode mode() {
    return DerivedAddressProvisionerMode.KEYSTORE;
  }

  @Override
  public List<String> createAddresses(String chain, String token, int count, String orderNo) {
    CryptoPaymentProperties.DerivedAddress.Keystore config = properties.getDerivedAddress().getKeystore();
    String password = secretValueResolver.resolveRequired(
        config.getPasswordSourceType(),
        config.getPassword(),
        config.getPasswordEnv(),
        config.getPasswordKmsKeyId(),
        "CRYPTO_PAYMENT_DERIVED_KEYSTORE_PASSWORD",
        "keystore password");
    List<String> addresses = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      ECKeyPair keyPair;
      try {
        keyPair = Keys.createEcKeyPair();
      } catch (Exception ex) {
        throw new IllegalStateException("Failed to generate EC key pair for keystore strategy", ex);
      }
      Credentials credentials = Credentials.create(keyPair);
      keystoreWriter.write(
          config.getOutputDir(),
          config.getFilePrefix(),
          credentials.getAddress(),
          keyPair.getPrivateKey(),
          password);
      addresses.add(credentials.getAddress());
    }
    log.info("Generated keystore derived addresses. chain={}, token={}, count={}, orderNo={}",
        chain, token, addresses.size(), orderNo);
    return addresses;
  }
}
