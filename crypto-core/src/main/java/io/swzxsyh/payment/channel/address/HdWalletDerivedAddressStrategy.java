package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.MnemonicUtils;

@Slf4j
@Component
public class HdWalletDerivedAddressStrategy implements DerivedAddressStrategy {

  private static final SecureRandom RANDOM = new SecureRandom();
  private final CryptoPaymentProperties properties;
  private final SecretValueResolver secretValueResolver;
  private final AtomicInteger cursor = new AtomicInteger();

  public HdWalletDerivedAddressStrategy(CryptoPaymentProperties properties, SecretValueResolver secretValueResolver) {
    this.properties = properties;
    this.secretValueResolver = secretValueResolver;
  }

  @Override
  public DerivedAddressProvisionerMode mode() {
    return DerivedAddressProvisionerMode.HD;
  }

  @Override
  public List<String> createAddresses(String chain, String token, int count, String orderNo) {
    CryptoPaymentProperties.DerivedAddress.Hd config = properties.getDerivedAddress().getHd();
    String mnemonic = secretValueResolver.resolveRequired(
        config.getMnemonicSourceType(),
        config.getMnemonic(),
        config.getMnemonicEnv(),
        config.getMnemonicKmsKeyId(),
        "CRYPTO_PAYMENT_DERIVED_HD_MNEMONIC",
        "HD mnemonic");
    if (!StringUtils.hasText(mnemonic)) {
      if (!config.isGenerateMnemonicWhenMissing()) {
        throw new IllegalStateException("HD mnemonic is not configured");
      }
      mnemonic = MnemonicUtils.generateMnemonic(randomEntropy());
      log.warn("HD mnemonic missing, generated temporary mnemonic for runtime use only.");
    }
    if (!MnemonicUtils.validateMnemonic(mnemonic)) {
      throw new IllegalArgumentException("Invalid HD mnemonic");
    }

    byte[] seed = MnemonicUtils.generateSeed(mnemonic, config.getPassphrase());
    Bip32ECKeyPair master = Bip32ECKeyPair.generateKeyPair(seed);
    List<String> addresses = new ArrayList<>(count);
    int baseIndex = Math.max(0, config.getStartIndex());
    String prefix = normalizePrefix(config.getDerivationPathPrefix());
    for (int i = 0; i < count; i++) {
      int index = baseIndex + cursor.getAndIncrement();
      int[] path = new int[] {
          harden(44),
          harden(60),
          harden(0),
          0,
          index
      };
      if (StringUtils.hasText(prefix)) {
        path = parsePath(prefix, index);
      }
      Bip32ECKeyPair derived = Bip32ECKeyPair.deriveKeyPair(master, path);
      BigInteger privateKey = derived.getPrivateKey();
      ECKeyPair keyPair = ECKeyPair.create(privateKey);
      Credentials credentials = Credentials.create(keyPair);
      addresses.add(credentials.getAddress());
    }
    log.info("Derived HD wallet addresses. chain={}, token={}, count={}, orderNo={}",
        chain, token, addresses.size(), orderNo);
    return addresses;
  }

  private byte[] randomEntropy() {
    byte[] entropy = new byte[16];
    RANDOM.nextBytes(entropy);
    return entropy;
  }

  private String normalizePrefix(String prefix) {
    return StringUtils.hasText(prefix) ? prefix.trim() : "m/44'/60'/0'/0";
  }

  private int harden(int value) {
    return value | 0x80000000;
  }

  private int[] parsePath(String prefix, int index) {
    String[] parts = prefix.split("/");
    List<Integer> path = new ArrayList<>();
    for (String part : parts) {
      if (part == null || part.isBlank() || "m".equalsIgnoreCase(part)) {
        continue;
      }
      boolean hardened = part.endsWith("'");
      String raw = hardened ? part.substring(0, part.length() - 1) : part;
      int value = Integer.parseInt(raw);
      path.add(hardened ? harden(value) : value);
    }
    path.add(index);
    return path.stream().mapToInt(Integer::intValue).toArray();
  }
}
