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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Int96;

/** Superfluid 流式支付合约适配器。 */
@Slf4j
@Component
public class SuperfluidSubscriptionContractAdapter extends AbstractWeb3SubscriptionContractAdapter {

  public SuperfluidSubscriptionContractAdapter(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      SecretValueResolver secretValueResolver) {
    super(properties, chainClientFactory, secretValueResolver);
  }

  @Override
  public SubscriptionBillingMode mode() {
    return SubscriptionBillingMode.SUPERFLUID_STREAM;
  }

  @Override
  public SubscriptionSetupPlan prepareSetup(
      SubscriptionOrder order, CreateSubscriptionOrderRequest request) {
    String cfa = properties.getSubscription().getSuperfluidCfaAddress();
    Function function =
        new Function(
            "createFlow",
            List.of(
                new Address(order.getTokenAddress()),
                new Address(request.recipientAddress()),
                new Int96(toFlowRatePerSecond(order)),
                new DynamicBytes(utf8Bytes(order.getSubscriptionOrderNo()))),
            List.of());
    String calldata = FunctionEncoder.encode(function);
    log.info("构造 Superfluid 流式支付初始化参数。subscriptionOrderNo={}, cfa={}",
        order.getSubscriptionOrderNo(), cfa);
    return new SubscriptionSetupPlan(
        mode(),
        cfa,
        calldata,
        "Superfluid CFA createFlow calldata.");
  }

  @Override
  public SubscriptionExecutionResult executeBilling(
      SubscriptionOrder order, SubscriptionBillingRecord bill) {
    return SubscriptionExecutionResult.skipped(
        "Superfluid stream is continuous; billing records are confirmed by stream/event observation.");
  }

  @Override
  public SubscriptionExecutionResult pause(SubscriptionOrder order) {
    return deleteFlow(order);
  }

  @Override
  public SubscriptionExecutionResult resume(SubscriptionOrder order) {
    Function function =
        new Function(
            "createFlow",
            List.of(
                new Address(order.getTokenAddress()),
                new Address(order.getRecipientAddress()),
                new Int96(toFlowRatePerSecond(order)),
                new DynamicBytes(utf8Bytes(order.getSubscriptionOrderNo()))),
            List.of());
    return sendExecutorTransaction(
        order, properties.getSubscription().getSuperfluidCfaAddress(), FunctionEncoder.encode(function));
  }

  @Override
  public SubscriptionExecutionResult cancel(SubscriptionOrder order) {
    return deleteFlow(order);
  }

  private SubscriptionExecutionResult deleteFlow(SubscriptionOrder order) {
    Function function =
        new Function(
            "deleteFlow",
            List.of(
                new Address(order.getTokenAddress()),
                new Address(order.getPayerAddress()),
                new Address(order.getRecipientAddress()),
                new DynamicBytes(utf8Bytes(order.getSubscriptionOrderNo()))),
            List.of());
    return sendExecutorTransaction(
        order, properties.getSubscription().getSuperfluidCfaAddress(), FunctionEncoder.encode(function));
  }

  private BigInteger toFlowRatePerSecond(SubscriptionOrder order) {
    int decimals =
        properties.getTokenProfiles().stream()
            .filter(profile -> order.getChain().equalsIgnoreCase(profile.getChain()))
            .filter(profile -> order.getToken().equalsIgnoreCase(profile.getToken()))
            .findFirst()
            .map(CryptoPaymentProperties.TokenProfile::getDecimals)
            .orElse(6);
    BigInteger amountPerCycle = order.getAmountPerCycle().movePointRight(decimals).toBigIntegerExact();
    return amountPerCycle.divide(BigInteger.valueOf(Math.max(1, order.getCycleSeconds())));
  }
}
