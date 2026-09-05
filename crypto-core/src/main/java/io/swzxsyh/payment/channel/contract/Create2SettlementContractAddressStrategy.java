package io.swzxsyh.payment.channel.contract;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

/** CREATE2 托管钱包合约地址策略。 */
@Slf4j
@Component
@Order(10)
public class Create2SettlementContractAddressStrategy implements SettlementContractAddressStrategy {

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;

  public Create2SettlementContractAddressStrategy(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
  }

  @Override
  public boolean supports(PaymentOrder order, PaymentSelection selection) {
    CryptoPaymentProperties.Contract contract = properties.getContract();
    if (!contract.isEnabled() || !contract.isCreate2Enabled()) {
      return false;
    }
    if (!StringUtils.hasText(contract.getCreate2FactoryAddress())
        || !StringUtils.hasText(contract.getCreate2InitCodeHash())) {
      return false;
    }
    if (contract.isCreate2HostedWalletOnly()) {
      return selection != null
          && selection.walletAccountType() != null
          && "SCA".equalsIgnoreCase(selection.walletAccountType().name());
    }
    return true;
  }

  @Override
  public ContractAddressPlan resolve(PaymentOrder order, PaymentSelection selection) {
    CryptoPaymentProperties.Contract contract = properties.getContract();
    String factoryAddress = normalizeAddress(contract.getCreate2FactoryAddress(), "create2FactoryAddress");
    String initCodeHash = normalizeHash(contract.getCreate2InitCodeHash(), "create2InitCodeHash");
    String salt = buildSalt(order, selection, contract.getCreate2SaltPrefix());
    String address = computeCreate2Address(factoryAddress, salt, initCodeHash);
    verifyCreate2Code(selection.chain(), factoryAddress, address);
    log.info(
        "Resolved CREATE2 settlement contract address. cryptoOrderNo={}, merchantId={}, chain={}, token={}, address={}",
        order.getCryptoOrderNo(),
        order.getMerchantId(),
        selection == null ? null : selection.chain(),
        selection == null ? null : selection.token(),
        address);
    return new ContractAddressPlan(address, mode(), factoryAddress, salt, initCodeHash);
  }

  @Override
  public String mode() {
    return "CREATE2";
  }

  private String buildSalt(PaymentOrder order, PaymentSelection selection, String prefix) {
    String seed = String.join(
        "|",
        StringUtils.hasText(prefix) ? prefix.trim() : "crypto:payment:escrow",
        safe(order == null ? null : order.getCryptoOrderNo()),
        safe(order == null ? null : order.getMerchantId()),
        safe(selection == null ? null : selection.chain()),
        safe(selection == null ? null : selection.token()),
        safe(selection == null ? null : selection.tokenAddress()),
        safe(selection == null || selection.walletAccountType() == null ? null : selection.walletAccountType().name()));
    return Numeric.toHexStringNoPrefix(Hash.sha3(seed.getBytes(StandardCharsets.UTF_8)));
  }

  private String computeCreate2Address(String factoryAddress, String saltHex, String initCodeHashHex) {
    byte[] factory = Numeric.hexStringToByteArray(factoryAddress);
    byte[] salt = Numeric.hexStringToByteArray(saltHex);
    byte[] initCodeHash = Numeric.hexStringToByteArray(initCodeHashHex);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(0xff);
    out.writeBytes(factory);
    out.writeBytes(salt);
    out.writeBytes(initCodeHash);
    byte[] hash = Hash.sha3(out.toByteArray());
    return "0x" + Numeric.toHexStringNoPrefix(hash).substring(24);
  }

  private void verifyCreate2Code(String chain, String factoryAddress, String predictedAddress) {
    ChainClient client = chainClientFactory.get(chain);
    if (!client.isEvmFamily()) {
      throw new IllegalStateException("CREATE2 settlement is only supported on EVM chains: " + chain);
    }
    verifyCodePresent(client, factoryAddress, "CREATE2 factory");
    verifyCodePresent(client, predictedAddress, "CREATE2 predicted escrow contract");
  }

  private void verifyCodePresent(ChainClient client, String address, String label) {
    String code = client.getCode(address);
    if (!StringUtils.hasText(code) || "0x".equalsIgnoreCase(code) || "0x0".equalsIgnoreCase(code)) {
      throw new IllegalStateException(label + " has no deployed bytecode: " + address);
    }
  }

  private String normalizeAddress(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    String normalized = value.trim();
    if (!normalized.matches("^0x[0-9a-fA-F]{40}$")) {
      throw new IllegalArgumentException(fieldName + " must be a valid EVM address");
    }
    return normalized;
  }

  private String normalizeHash(String value, String fieldName) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    String normalized = value.trim();
    if (!normalized.startsWith("0x")) {
      normalized = "0x" + normalized;
    }
    if (!normalized.matches("^0x[0-9a-fA-F]{64}$")) {
      throw new IllegalArgumentException(fieldName + " must be a 32-byte hex hash");
    }
    return normalized;
  }

  private String safe(String value) {
    return StringUtils.hasText(value) ? value.trim() : "";
  }
}
