package io.swzxsyh.payment.subscription.contract;

import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.model.SubscriptionSetupPlan;
import io.swzxsyh.payment.util.SecretValueResolver;
import java.math.BigInteger;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;

/** ERC-1337 / EIP-945 类周期扣款合约适配器。 */
@Slf4j
@Component
public class Erc1337SubscriptionContractAdapter extends AbstractWeb3SubscriptionContractAdapter {

  public Erc1337SubscriptionContractAdapter(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      SecretValueResolver secretValueResolver) {
    super(properties, chainClientFactory, secretValueResolver);
  }

  @Override
  public SubscriptionBillingMode mode() {
    return SubscriptionBillingMode.ERC_1337;
  }

  @Override
  public SubscriptionSetupPlan prepareSetup(
      SubscriptionOrder order, CreateSubscriptionOrderRequest request) {
    String executor = properties.getSubscription().getErc1337ExecutorAddress();
    Function function =
        new Function(
            "createSubscription",
            List.of(
                new Bytes32(orderIdBytes32(order.getSubscriptionOrderNo())),
                new Address(order.getTokenAddress()),
                new Address(request.payerAddress()),
                new Address(request.recipientAddress()),
                new Uint256(toTokenUnit(order)),
                new Uint256(BigInteger.valueOf(order.getCycleSeconds())),
                new Uint256(BigInteger.valueOf(order.getNextBillingAt()
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond())),
                new DynamicBytes(utf8Bytes(order.getMerchantOrderNo()))),
            List.of());
    String calldata = FunctionEncoder.encode(function);
    log.info("构造 ERC-1337 订阅初始化参数。subscriptionOrderNo={}, executor={}",
        order.getSubscriptionOrderNo(), executor);
    return new SubscriptionSetupPlan(
        mode(),
        executor,
        calldata,
        "ERC-1337/EIP-945 style contract calldata for createSubscription.");
  }

  @Override
  public SubscriptionExecutionResult executeBilling(
      SubscriptionOrder order, SubscriptionBillingRecord bill) {
    Function function =
        new Function(
            "executeSubscription",
            List.of(
                new Bytes32(orderIdBytes32(order.getSubscriptionOrderNo())),
                new Uint256(BigInteger.valueOf(bill.getBillingSequence()))),
            List.of());
    return sendExecutorTransaction(
        order, properties.getSubscription().getErc1337ExecutorAddress(), FunctionEncoder.encode(function));
  }

  @Override
  public SubscriptionExecutionResult pause(SubscriptionOrder order) {
    return sendOrderOnlyFunction(order, "pauseSubscription");
  }

  @Override
  public SubscriptionExecutionResult resume(SubscriptionOrder order) {
    return sendOrderOnlyFunction(order, "resumeSubscription");
  }

  @Override
  public SubscriptionExecutionResult cancel(SubscriptionOrder order) {
    return sendOrderOnlyFunction(order, "cancelSubscription");
  }

  private SubscriptionExecutionResult sendOrderOnlyFunction(SubscriptionOrder order, String method) {
    Function function =
        new Function(method, List.of(new Bytes32(orderIdBytes32(order.getSubscriptionOrderNo()))), List.of());
    return sendExecutorTransaction(
        order, properties.getSubscription().getErc1337ExecutorAddress(), FunctionEncoder.encode(function));
  }

  private BigInteger toTokenUnit(SubscriptionOrder order) {
    int decimals =
        properties.getTokenProfiles().stream()
            .filter(profile -> order.getChain().equalsIgnoreCase(profile.getChain()))
            .filter(profile -> order.getToken().equalsIgnoreCase(profile.getToken()))
            .findFirst()
            .map(CryptoPaymentProperties.TokenProfile::getDecimals)
            .orElse(6);
    return order.getAmountPerCycle().movePointRight(decimals).toBigIntegerExact();
  }
}
