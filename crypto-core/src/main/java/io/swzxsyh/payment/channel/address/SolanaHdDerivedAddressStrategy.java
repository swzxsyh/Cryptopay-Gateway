package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import org.p2p.solanaj.core.PublicKey;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.crypto.MnemonicUtils;

/** Solana HD 地址派生策略，使用 BIP39 seed + SLIP-0010 ed25519 hardened path。 */
@Slf4j
@Component
public class SolanaHdDerivedAddressStrategy implements DerivedAddressStrategy {

  private static final byte[] ED25519_SEED = "ed25519 seed".getBytes(java.nio.charset.StandardCharsets.UTF_8);
  private final CryptoPaymentProperties properties;
  private final SecretValueResolver secretValueResolver;
  private final AtomicInteger cursor = new AtomicInteger();

  public SolanaHdDerivedAddressStrategy(
      CryptoPaymentProperties properties,
      SecretValueResolver secretValueResolver) {
    this.properties = properties;
    this.secretValueResolver = secretValueResolver;
  }

  @Override
  public DerivedAddressProvisionerMode mode() {
    return DerivedAddressProvisionerMode.SOLANA_HD;
  }

  @Override
  public List<String> createAddresses(String chain, String token, int count, String orderNo) {
    if (ChainFamilyResolver.resolve(chain) != ChainFamily.SOLANA) {
      throw new IllegalArgumentException("SOLANA_HD strategy only supports Solana chain: " + chain);
    }
    CryptoPaymentProperties.DerivedAddress.Hd config = properties.getDerivedAddress().getHd();
    String mnemonic =
        secretValueResolver.resolveRequired(
            config.getMnemonicSourceType(),
            config.getMnemonic(),
            config.getMnemonicEnv(),
            config.getMnemonicKmsKeyId(),
            "CRYPTO_PAYMENT_DERIVED_HD_MNEMONIC",
            "Solana HD mnemonic");
    if (!MnemonicUtils.validateMnemonic(mnemonic)) {
      throw new IllegalArgumentException("Invalid Solana HD mnemonic");
    }

    byte[] seed = MnemonicUtils.generateSeed(mnemonic, config.getPassphrase());
    int baseIndex = Math.max(0, config.getStartIndex());
    String pathPrefix = solanaPathPrefix(config.getDerivationPathPrefix());
    List<String> addresses = new ArrayList<>(Math.max(0, count));
    for (int i = 0; i < count; i++) {
      int index = baseIndex + cursor.getAndIncrement();
      byte[] privateKeySeed = derivePrivateKeySeed(seed, parsePath(pathPrefix, index));
      addresses.add(publicKey(privateKeySeed));
    }
    log.info(
        "Derived Solana HD wallet addresses. chain={}, token={}, count={}, pathPrefix={}, orderNo={}",
        chain,
        token,
        addresses.size(),
        pathPrefix,
        orderNo);
    return addresses;
  }

  private String solanaPathPrefix(String configuredPrefix) {
    String prefix = StringUtils.hasText(configuredPrefix) ? configuredPrefix.trim() : "";
    if (!StringUtils.hasText(prefix) || prefix.contains("/60'")) {
      return "m/44'/501'/0'/0'";
    }
    return prefix;
  }

  private int[] parsePath(String prefix, int index) {
    String[] parts = prefix.split("/");
    List<Integer> path = new ArrayList<>();
    for (String part : parts) {
      if (!StringUtils.hasText(part) || "m".equalsIgnoreCase(part)) {
        continue;
      }
      if (!part.endsWith("'")) {
        throw new IllegalArgumentException("Solana SLIP-0010 only supports hardened path segment: " + prefix);
      }
      String raw = part.substring(0, part.length() - 1);
      path.add(harden(Integer.parseInt(raw)));
    }
    path.add(harden(index));
    return path.stream().mapToInt(Integer::intValue).toArray();
  }

  private byte[] derivePrivateKeySeed(byte[] seed, int[] path) {
    Slip10Node node = masterNode(seed);
    for (int childNumber : path) {
      node = childNode(node, childNumber);
    }
    return node.key();
  }

  private Slip10Node masterNode(byte[] seed) {
    byte[] digest = hmacSha512(ED25519_SEED, seed);
    return new Slip10Node(Arrays.copyOfRange(digest, 0, 32), Arrays.copyOfRange(digest, 32, 64));
  }

  private Slip10Node childNode(Slip10Node parent, int childNumber) {
    ByteBuffer data = ByteBuffer.allocate(1 + 32 + 4).order(ByteOrder.BIG_ENDIAN);
    data.put((byte) 0);
    data.put(parent.key());
    data.putInt(childNumber);
    byte[] digest = hmacSha512(parent.chainCode(), data.array());
    return new Slip10Node(Arrays.copyOfRange(digest, 0, 32), Arrays.copyOfRange(digest, 32, 64));
  }

  private byte[] hmacSha512(byte[] key, byte[] data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(new SecretKeySpec(key, "HmacSHA512"));
      return mac.doFinal(data);
    } catch (InvalidKeyException | java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Failed to derive Solana SLIP-0010 key", ex);
    }
  }

  private String publicKey(byte[] privateKeySeed) {
    var spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);
    EdDSAPrivateKeySpec privateKeySpec = new EdDSAPrivateKeySpec(privateKeySeed, spec);
    return new PublicKey(privateKeySpec.getA().toByteArray()).toBase58();
  }

  private int harden(int value) {
    return value | 0x80000000;
  }

  private record Slip10Node(byte[] key, byte[] chainCode) {}
}
