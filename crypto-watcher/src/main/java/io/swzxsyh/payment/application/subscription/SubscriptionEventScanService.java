package io.swzxsyh.payment.application.subscription;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.subscription.SubscriptionBillingService;
import io.swzxsyh.payment.subscription.SubscriptionOrderService;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.utils.Numeric;

/**
 * Scans EVM subscription contract events.
 *
 * <p>The scanner is payment-application specific because it is driven by the payment chain watcher.
 * Business state changes are still delegated to core subscription services.
 */
@Slf4j
@Service
public class SubscriptionEventScanService {

  private static final String EVENT_SUBSCRIPTION_ACTIVATED =
      Hash.sha3String("SubscriptionActivated(bytes32,address,address)");
  private static final String EVENT_SUBSCRIPTION_BILLING_PAID =
      Hash.sha3String("SubscriptionBillingPaid(bytes32,uint256,address,uint256)");
  private static final String EVENT_SUBSCRIPTION_PAUSED =
      Hash.sha3String("SubscriptionPaused(bytes32)");
  private static final String EVENT_SUBSCRIPTION_RESUMED =
      Hash.sha3String("SubscriptionResumed(bytes32)");
  private static final String EVENT_SUBSCRIPTION_CANCELLED =
      Hash.sha3String("SubscriptionCancelled(bytes32)");

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final SubscriptionOrderService orderService;
  private final SubscriptionBillingService billingService;

  /**
   * Creates a subscription event scanner.
   *
   * @param properties payment runtime properties
   * @param chainClientFactory chain client factory
   * @param orderService subscription order service
   * @param billingService subscription billing service
   */
  public SubscriptionEventScanService(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      SubscriptionOrderService orderService,
      SubscriptionBillingService billingService) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.orderService = orderService;
    this.billingService = billingService;
  }

  /**
   * Scans subscription contract logs in the given confirmed block range.
   *
   * @param chain chain code
   * @param fromBlock inclusive start block
   * @param toBlock inclusive end block
   */
  public void scan(String chain, long fromBlock, long toBlock) {
    if (!properties.getSubscription().isEnabled() || fromBlock > toBlock) {
      return;
    }
    List<String> contracts = subscriptionContracts();
    if (contracts.isEmpty()) {
      log.debug("未配置订阅合约地址，跳过订阅事件扫描。chain={}", chain);
      return;
    }

    ChainClient client = chainClientFactory.get(chain);
    if (!client.isEvmFamily()) {
      log.debug("非 EVM 链暂不执行订阅合约事件扫描。chain={}, family={}, fromBlock={}, toBlock={}",
          chain, client.family(), fromBlock, toBlock);
      return;
    }
    for (String contract : contracts) {
      EthFilter filter =
          new EthFilter(
              new DefaultBlockParameterNumber(BigInteger.valueOf(fromBlock)),
              new DefaultBlockParameterNumber(BigInteger.valueOf(toBlock)),
              contract);
      EthLog logs = client.getLogs(filter);
      if (logs == null || logs.getLogs() == null || logs.getLogs().isEmpty()) {
        continue;
      }
      log.info("扫描到订阅合约日志。chain={}, contract={}, count={}, fromBlock={}, toBlock={}",
          chain, contract, logs.getLogs().size(), fromBlock, toBlock);
      handleLogs(logs);
    }
  }

  private void handleLogs(EthLog logs) {
    for (EthLog.LogResult<?> result : logs.getLogs()) {
      if (!(result.get() instanceof EthLog.LogObject logObject)
          || !StringUtils.hasText(logObject.getTransactionHash())) {
        continue;
      }
      ParsedSubscriptionEvent event = parseEvent(logObject);
      if (event == null) {
        log.debug("跳过未知订阅合约日志。txHash={}, address={}, topics={}",
            logObject.getTransactionHash(), logObject.getAddress(), logObject.getTopics());
        continue;
      }
      String txHash = logObject.getTransactionHash();
      Long blockNumber =
          logObject.getBlockNumber() == null ? null : logObject.getBlockNumber().longValue();
      boolean activated =
          event.type() == SubscriptionEventType.ACTIVATED
              && (orderService.activateBySetupTxHash(txHash)
                  || orderService.activateByEventId(event.subscriptionId()));
      boolean billingPaid =
          event.type() == SubscriptionEventType.BILLING_PAID
              && billingService.confirmByTxHash(txHash, blockNumber);
      boolean statusChanged = switch (event.type()) {
        case PAUSED -> orderService.pauseByEventId(event.subscriptionId());
        case RESUMED -> orderService.resumeByEventId(event.subscriptionId());
        case CANCELLED -> orderService.cancelByEventId(event.subscriptionId());
        default -> false;
      };
      if (activated || billingPaid || statusChanged) {
        log.info("订阅合约日志已精确匹配业务记录。txHash={}, eventType={}, subscriptionId={}, sequence={}, amountRaw={}, activated={}, billingPaid={}, statusChanged={}",
            txHash, event.type(), event.subscriptionId(), event.sequence(), event.amountRaw(), activated, billingPaid, statusChanged);
      } else {
        log.debug("订阅合约日志已识别但未匹配业务记录。txHash={}, eventType={}, subscriptionId={}, sequence={}, amountRaw={}",
            txHash, event.type(), event.subscriptionId(), event.sequence(), event.amountRaw());
      }
    }
  }

  private ParsedSubscriptionEvent parseEvent(EthLog.LogObject logObject) {
    List<String> topics = logObject.getTopics();
    if (topics == null || topics.isEmpty() || !StringUtils.hasText(topics.get(0))) {
      return null;
    }
    String topic0 = topics.get(0);
    if (EVENT_SUBSCRIPTION_ACTIVATED.equalsIgnoreCase(topic0)) {
      return new ParsedSubscriptionEvent(
          SubscriptionEventType.ACTIVATED,
          topic(topics, 1),
          null,
          null);
    }
    if (EVENT_SUBSCRIPTION_BILLING_PAID.equalsIgnoreCase(topic0)) {
      return new ParsedSubscriptionEvent(
          SubscriptionEventType.BILLING_PAID,
          topic(topics, 1),
          parseUint256Topic(topics, 2),
          parseUint256Data(logObject.getData(), 0));
    }
    if (EVENT_SUBSCRIPTION_PAUSED.equalsIgnoreCase(topic0)) {
      return new ParsedSubscriptionEvent(SubscriptionEventType.PAUSED, topic(topics, 1), null, null);
    }
    if (EVENT_SUBSCRIPTION_RESUMED.equalsIgnoreCase(topic0)) {
      return new ParsedSubscriptionEvent(SubscriptionEventType.RESUMED, topic(topics, 1), null, null);
    }
    if (EVENT_SUBSCRIPTION_CANCELLED.equalsIgnoreCase(topic0)) {
      return new ParsedSubscriptionEvent(SubscriptionEventType.CANCELLED, topic(topics, 1), null, null);
    }
    return null;
  }

  private String topic(List<String> topics, int index) {
    return topics.size() > index ? topics.get(index) : null;
  }

  private BigInteger parseUint256Topic(List<String> topics, int index) {
    String value = topic(topics, index);
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return Numeric.toBigInt(value);
  }

  private BigInteger parseUint256Data(String data, int slot) {
    if (!StringUtils.hasText(data)) {
      return null;
    }
    String clean = Numeric.cleanHexPrefix(data);
    int start = slot * 64;
    int end = start + 64;
    if (clean.length() < end) {
      return null;
    }
    return Numeric.toBigInt(clean.substring(start, end));
  }

  private List<String> subscriptionContracts() {
    Set<String> contracts = new LinkedHashSet<>();
    addIfText(contracts, properties.getSubscription().getErc1337ExecutorAddress());
    addIfText(contracts, properties.getSubscription().getSuperfluidCfaAddress());
    addIfText(contracts, properties.getSubscription().getSuperfluidHostAddress());
    return new ArrayList<>(contracts);
  }

  private void addIfText(Set<String> values, String value) {
    if (StringUtils.hasText(value)) {
      values.add(value.trim());
    }
  }

  private enum SubscriptionEventType {
    ACTIVATED,
    BILLING_PAID,
    PAUSED,
    RESUMED,
    CANCELLED
  }

  private record ParsedSubscriptionEvent(
      SubscriptionEventType type,
      String subscriptionId,
      BigInteger sequence,
      BigInteger amountRaw) {}
}
