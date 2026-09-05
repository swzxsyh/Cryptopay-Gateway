package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.gas.GasPayerMode;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.routing.TokenRouteType;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.utils.Numeric;

/** EVM EIP-3009 代付实现：用户签授权，平台 relayer 调用 token 合约并支付 gas。 */
@Component
public class Eip3009GasSponsorProvider implements GasSponsorProvider {

  private static final String DEFAULT_RELAYER_KEY_ENV = "CRYPTO_PAYMENT_EVM_RELAYER_PRIVATE_KEY";

  private final CryptoPaymentProperties properties;
  private final GasSponsorshipPolicyService policyService;
  private final ChainClientFactory chainClientFactory;
  private final SecretValueResolver secretValueResolver;

  public Eip3009GasSponsorProvider(
      CryptoPaymentProperties properties,
      GasSponsorshipPolicyService policyService,
      ChainClientFactory chainClientFactory,
      SecretValueResolver secretValueResolver) {
    this.properties = properties;
    this.policyService = policyService;
    this.chainClientFactory = chainClientFactory;
    this.secretValueResolver = secretValueResolver;
  }

  @Override
  public String providerId() {
    return "EIP3009";
  }

  @Override
  public boolean supports(GasSponsorContext context) {
    if (context == null || context.selection() == null || context.tokenRoutePlan() == null) {
      return false;
    }
    return ChainFamilyResolver.resolve(context.selection().chain()) == ChainFamily.EVM
        && context.selection().paymentMethod() == PaymentMethod.CONTRACT
        && context.tokenRoutePlan().routeType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION
        && tokenSupportsEip3009(context.selection().chain(), context.selection().token())
        && policyService.isSponsorEnabled(context.selection().chain(), providerId());
  }

  @Override
  public GasSponsorPlan plan(GasSponsorContext context) {
    String expectedTo = expectedRecipient(context.order());
    String payload = "{\"scheme\":\"EIP3009\","
        + "\"tokenAddress\":\"" + context.selection().tokenAddress() + "\","
        + "\"from\":\"" + nullToEmpty(context.selection().walletAddress()) + "\","
        + "\"to\":\"" + expectedTo + "\","
        + "\"value\":\"" + expectedAtomicAmount(context.order().getAmount(), context.selection().chain(), context.selection().token()) + "\","
        + "\"submitEndpoint\":\"/api/crypto/evm/sponsor/orders/"
        + context.order().getCryptoOrderNo() + "/eip3009/submit\"}";
    return new GasSponsorPlan(
        true,
        true,
        providerId(),
        GasPayerMode.PLATFORM_SPONSORED,
        "token supports EIP-3009; platform relayer can submit transferWithAuthorization",
        true,
        "EIP712_TRANSFER_WITH_AUTHORIZATION",
        payload);
  }

  @Override
  public GasSponsorExecutionResult execute(GasSponsorExecutionCommand command) {
    if (command == null || command.order() == null || command.eip3009Authorization() == null) {
      throw new IllegalArgumentException("EIP-3009 authorization is required");
    }
    validate(command);
    ChainClient chainClient = chainClientFactory.get(command.order().getChain());
    Eip3009AuthorizationSubmission authorization = command.eip3009Authorization();
    String data = encodeTransferWithAuthorization(authorization);
    String relayerPrivateKey = resolveRelayerPrivateKey();
    BigInteger gasLimit = BigInteger.valueOf(
        policyService.sponsorGasLimit(
            command.order().getChain(),
            properties.getGas().getPlatformSponsoredGasLimit()));
    String txHash = chainClient.sendTransaction(
        relayerPrivateKey,
        command.order().getTokenAddress(),
        data,
        BigInteger.ZERO,
        gasLimit);
    return new GasSponsorExecutionResult(true, txHash, providerId(), "EIP-3009 relayer transaction submitted");
  }

  private void validate(GasSponsorExecutionCommand command) {
    Eip3009AuthorizationSubmission authorization = command.eip3009Authorization();
    if (command.order().getStatus() != OrderStatus.WAITING_PAYMENT
        && command.order().getStatus() != OrderStatus.DETECTED) {
      throw new IllegalStateException("Order is not payable: " + command.order().getStatus());
    }
    requireEquals(command.order().getChain(), authorization.chain(), "chain");
    requireEquals(command.order().getTokenAddress(), authorization.tokenAddress(), "tokenAddress");
    requireEquals(command.order().getWalletAddress(), authorization.from(), "from wallet");
    requireEquals(expectedRecipient(command.order()), authorization.to(), "recipient");
    if (authorization.value() == null
        || authorization.validAfter() == null
        || authorization.validBefore() == null) {
      throw new IllegalArgumentException("EIP-3009 value and valid window are required");
    }
    if (authorization.validBefore().compareTo(authorization.validAfter()) <= 0) {
      throw new IllegalArgumentException("EIP-3009 validBefore must be greater than validAfter");
    }
    if (authorization.validBefore().compareTo(BigInteger.valueOf(Instant.now().getEpochSecond())) <= 0) {
      throw new IllegalArgumentException("EIP-3009 authorization has expired");
    }
    BigInteger expectedValue =
        expectedAtomicAmount(command.order().getAmount(), command.order().getChain(), command.order().getToken());
    if (!expectedValue.equals(authorization.value())) {
      throw new IllegalArgumentException("EIP-3009 value does not match order amount");
    }
    if (!StringUtils.hasText(authorization.nonce())
        || Numeric.hexStringToByteArray(authorization.nonce()).length != 32) {
      throw new IllegalArgumentException("EIP-3009 nonce must be bytes32 hex");
    }
    if (authorization.v() == null
        || !StringUtils.hasText(authorization.r())
        || !StringUtils.hasText(authorization.s())) {
      throw new IllegalArgumentException("EIP-3009 signature v/r/s is required");
    }
    if (Numeric.hexStringToByteArray(authorization.r()).length != 32
        || Numeric.hexStringToByteArray(authorization.s()).length != 32) {
      throw new IllegalArgumentException("EIP-3009 signature r/s must be bytes32 hex");
    }
  }

  private String encodeTransferWithAuthorization(Eip3009AuthorizationSubmission authorization) {
    Function function = new Function(
        "transferWithAuthorization",
        List.of(
            new Address(authorization.from()),
            new Address(authorization.to()),
            new Uint256(authorization.value()),
            new Uint256(authorization.validAfter()),
            new Uint256(authorization.validBefore()),
            new Bytes32(Numeric.hexStringToByteArray(authorization.nonce())),
            new Uint8(BigInteger.valueOf(authorization.v())),
            new Bytes32(Numeric.hexStringToByteArray(authorization.r())),
            new Bytes32(Numeric.hexStringToByteArray(authorization.s()))),
        List.of());
    return FunctionEncoder.encode(function);
  }

  private String resolveRelayerPrivateKey() {
    CryptoPaymentProperties.Gas gas = properties.getGas();
    return secretValueResolver.resolveRequired(
        gas.getEvmRelayerPrivateKeySourceType(),
        gas.getEvmRelayerPrivateKey(),
        gas.getEvmRelayerPrivateKeyEnv(),
        gas.getEvmRelayerPrivateKeyKmsKeyId(),
        DEFAULT_RELAYER_KEY_ENV,
        "EVM relayer private key");
  }

  private boolean tokenSupportsEip3009(String chain, String token) {
    return properties.getTokenProfiles().stream()
        .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
        .filter(profile -> token.equalsIgnoreCase(profile.getToken()))
        .anyMatch(CryptoPaymentProperties.TokenProfile::isTransferWithAuthorization);
  }

  private BigInteger expectedAtomicAmount(BigDecimal amount, String chain, String token) {
    int decimals = tokenDecimals(chain, token);
    return amount.movePointRight(decimals).setScale(0).toBigIntegerExact();
  }

  private int tokenDecimals(String chain, String token) {
    return properties.getTokenProfiles().stream()
        .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
        .filter(profile -> token.equalsIgnoreCase(profile.getToken()))
        .findFirst()
        .map(CryptoPaymentProperties.TokenProfile::getDecimals)
        .orElse(6);
  }

  private String expectedRecipient(PaymentOrder order) {
    if (StringUtils.hasText(order.getContractAddress())) {
      return order.getContractAddress();
    }
    if (StringUtils.hasText(order.getPaymentAddress())) {
      return order.getPaymentAddress();
    }
    throw new IllegalStateException("Order recipient is not prepared");
  }

  private void requireEquals(String expected, String actual, String fieldName) {
    if (!StringUtils.hasText(expected) || !StringUtils.hasText(actual) || !expected.equalsIgnoreCase(actual)) {
      throw new IllegalArgumentException("EIP-3009 " + fieldName + " does not match order");
    }
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
