package io.swzxsyh.payment.subscription.contract;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionEventId;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.util.StringUtils;
import org.web3j.utils.Numeric;

/** Web3j 合约适配器公共能力。 */
abstract class AbstractWeb3SubscriptionContractAdapter implements SubscriptionContractAdapter {

  protected final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final SecretValueResolver secretValueResolver;

  protected AbstractWeb3SubscriptionContractAdapter(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      SecretValueResolver secretValueResolver) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.secretValueResolver = secretValueResolver;
  }

  protected SubscriptionExecutionResult sendExecutorTransaction(
      SubscriptionOrder order, String to, String data) {
    if (!StringUtils.hasText(to) || !StringUtils.hasText(data)) {
      return SubscriptionExecutionResult.skipped("contract address or calldata is blank");
    }
    String privateKey =
        secretValueResolver.resolveOptional(
            properties.getSubscription().getExecutorPrivateKeySourceType(),
            properties.getSubscription().getExecutorPrivateKey(),
            properties.getSubscription().getExecutorPrivateKeyEnv(),
            properties.getSubscription().getExecutorPrivateKeyKmsKeyId(),
            "CRYPTO_PAYMENT_SUBSCRIPTION_EXECUTOR_PRIVATE_KEY");
    if (!StringUtils.hasText(privateKey)) {
      return SubscriptionExecutionResult.skipped("subscription executor private key is not configured");
    }
    String txHash =
        chainClientFactory
            .get(order.getChain())
            .sendTransaction(
                Numeric.cleanHexPrefix(privateKey),
                to,
                data,
                BigInteger.ZERO,
                properties.getSubscription().getExecutionGasLimit());
    return SubscriptionExecutionResult.submitted(txHash);
  }

  protected byte[] orderIdBytes32(String orderNo) {
    byte[] hash = Numeric.hexStringToByteArray(SubscriptionEventId.fromOrderNo(orderNo));
    return Arrays.copyOf(hash, 32);
  }

  protected byte[] utf8Bytes(String value) {
    return (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
  }
}
