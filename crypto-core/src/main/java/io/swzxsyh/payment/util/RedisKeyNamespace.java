package io.swzxsyh.payment.util;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * Redis key namespace factory.
 *
 * <p>All payment runtime Redis keys should be created here instead of being assembled with scattered
 * string literals. The root namespace is configurable through {@code crypto.payment.redis.key-prefix}
 * and defaults to {@code crypto:payment}.
 */
public final class RedisKeyNamespace {

  /** Default root namespace for all payment runtime Redis keys. */
  public static final String DEFAULT_ROOT = "crypto:payment";

  /** Suffix for available addresses in one derived-address pool. */
  public static final String DERIVED_ADDRESS_AVAILABLE_SUFFIX = "available";

  /** Suffix for leased addresses in one derived-address pool. */
  public static final String DERIVED_ADDRESS_LEASES_SUFFIX = "leases";

  /** Suffix for the lock guarding one derived-address pool. */
  public static final String DERIVED_ADDRESS_LOCK_SUFFIX = "lock";

  private RedisKeyNamespace() {}

  /** Returns the configured root namespace, falling back to {@link #DEFAULT_ROOT}. */
  public static String root(CryptoPaymentProperties properties) {
    if (properties == null || properties.getRedis() == null) {
      return DEFAULT_ROOT;
    }
    return root(properties.getRedis().getKeyPrefix());
  }

  /** Normalizes a configured Redis root namespace. */
  public static String root(String configuredRoot) {
    String root = StringUtils.hasText(configuredRoot) ? configuredRoot.trim() : DEFAULT_ROOT;
    while (root.endsWith(":")) {
      root = root.substring(0, root.length() - 1);
    }
    return root;
  }

  /** Address-pool key for one chain and one token. Stored in DB and used as the Redis pool prefix. */
  public static String derivedAddressPool(CryptoPaymentProperties properties, String chain, String token) {
    return root(properties) + ":derived-address:" + upper(chain, "UNKNOWN_CHAIN") + ":" + upper(token, "UNKNOWN_TOKEN");
  }

  /** Redis set storing destination addresses that still need chain-log matching for one chain/token. */
  public static String activeDestinationAddresses(
      CryptoPaymentProperties properties, String chain, String tokenAddress) {
    return root(properties)
        + ":active-destination:"
        + upper(chain, "UNKNOWN_CHAIN")
        + ":"
        + lower(tokenAddress, "native");
  }

  /** Redis hash key storing scanner checkpoint hot progress for one chain. */
  public static String scannerCheckpoint(CryptoPaymentProperties properties, String chain) {
    return root(properties) + ":scanner:checkpoint:" + upper(chain, "UNKNOWN_CHAIN");
  }

  /** Distributed lock for the EVM block scanner of one chain. */
  public static String evmScannerLock(CryptoPaymentProperties properties, String chain) {
    return root(properties) + ":scanner:lock:" + upper(chain, "UNKNOWN_CHAIN");
  }

  /** Distributed lock for ERC20 eth_getLogs scanning of one chain. */
  public static String erc20ScannerLock(CryptoPaymentProperties properties, String chain) {
    return root(properties) + ":scanner:erc20:" + upper(chain, "UNKNOWN_CHAIN");
  }

  /** Distributed lock for Solana polling of one chain. */
  public static String solanaScannerLock(CryptoPaymentProperties properties, String chain) {
    return root(properties) + ":scanner:solana:" + upper(chain, "UNKNOWN_CHAIN");
  }

  /** WSS leader election key for one EVM chain. */
  public static String websocketLeader(CryptoPaymentProperties properties, String chain) {
    return root(properties) + ":watcher:wss:leader:" + upper(chain, "UNKNOWN_CHAIN");
  }

  /** Redis idempotency result bucket for one API idempotency scope and key. */
  public static String idempotencyBucket(CryptoPaymentProperties properties, String scope, String idempotencyKey) {
    return root(properties) + ":idem:" + lower(scope, "default") + ":" + lower(idempotencyKey, "");
  }

  /** Redis tx replay guard key. */
  public static String replayTx(CryptoPaymentProperties properties, String chain, String txHash) {
    return root(properties) + ":replay:tx:" + upper(chain, "UNKNOWN_CHAIN") + ":" + lower(txHash, "");
  }

  /** Redis business replay guard key. */
  public static String replayOrder(CryptoPaymentProperties properties, String scope, String key) {
    return root(properties) + ":replay:order:" + upper(scope, "DEFAULT") + ":" + lower(key, "");
  }

  /** Distributed lock for DB idempotency record mutation. */
  public static String dbIdempotencyLockPrefix(CryptoPaymentProperties properties) {
    return root(properties) + ":db:idempotency:";
  }

  /** Distributed lock for DB replay record mutation. */
  public static String dbReplayLock(CryptoPaymentProperties properties, String chain, String txHash) {
    return root(properties) + ":db:replay:" + upper(chain, "UNKNOWN_CHAIN") + ":" + lower(txHash, "");
  }

  /** Distributed lock for a callback delivery record. */
  public static String callbackDispatchLock(CryptoPaymentProperties properties, Long recordId) {
    return root(properties) + ":callback:dispatch:" + recordId;
  }

  /** Distributed lock for crediting one payment order from an on-chain event. */
  public static String creditOrderLock(CryptoPaymentProperties properties, String cryptoOrderNo) {
    return root(properties) + ":credit-order:" + upper(cryptoOrderNo, "UNKNOWN_ORDER");
  }

  /** Distributed lock for posting one merchant balance ledger entry. */
  public static String merchantBalancePostingLock(CryptoPaymentProperties properties, String bizType, String bizNo) {
    return root(properties)
        + ":merchant-balance:posting:"
        + upper(bizType, "UNKNOWN_BIZ")
        + ":"
        + upper(bizNo, "UNKNOWN_NO");
  }

  /** Distributed lock for one normalized chain-payment event. */
  public static String chainPaymentEventLock(CryptoPaymentProperties properties, String chain, String txHash) {
    return root(properties) + ":chain-event:" + upper(chain, "UNKNOWN_CHAIN") + ":" + lower(txHash, "");
  }

  /** Distributed lock for the subscription billing scheduler. */
  public static String subscriptionBillingSchedulerLock(CryptoPaymentProperties properties) {
    return root(properties) + ":subscription:billing:scheduler";
  }

  /** Distributed lock for subscription receipt confirmation. */
  public static String subscriptionReceiptConfirmationLock(CryptoPaymentProperties properties) {
    return root(properties) + ":subscription:receipt:confirm";
  }

  /** Distributed lock guarding sponsored gas submission for one order. */
  public static String gasSponsorOrderLock(CryptoPaymentProperties properties, String cryptoOrderNo) {
    return root(properties) + ":gas-sponsor:lock:order:" + upper(cryptoOrderNo, "UNKNOWN_ORDER");
  }

  /** Redis counter key for sponsored gas abuse protection. */
  public static String gasSponsorQuota(
      CryptoPaymentProperties properties, String providerId, String requestType, String dimension, String value) {
    return root(properties)
        + ":gas-sponsor:quota:"
        + lower(providerId, "default-provider")
        + ":"
        + lower(requestType, "default-request")
        + ":"
        + lower(dimension, "dimension")
        + ":"
        + lower(value, "");
  }

  /** Redis hash key exposing pending confirmation progress to the cashier page. */
  public static String pendingConfirmation(
      CryptoPaymentProperties properties, String chain, String txHash, Number logIndex) {
    return root(properties)
        + ":pending-confirmation:"
        + upper(chain, "UNKNOWN_CHAIN")
        + ":"
        + lower(txHash, "")
        + ":"
        + (logIndex == null ? 0 : logIndex);
  }

  private static String upper(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : fallback;
  }

  private static String lower(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : fallback;
  }
}
