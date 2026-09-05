package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.GasSponsorAttemptRecordMapper;
import io.swzxsyh.payment.persistence.entity.GasSponsorAttemptRecord;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.payment.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Gas 代付防刷入口，按订单、钱包、IP、cashierToken 做客户维度限频和审计。 */
@Service
public class GasSponsorGuardService {

  private final CryptoPaymentProperties properties;
  private final RedisUtil redisUtil;
  private final LockUtil lockUtil;
  private final GasSponsorAttemptRecordMapper recordMapper;
  private final HttpServletRequest request;

  public GasSponsorGuardService(
      CryptoPaymentProperties properties,
      RedisUtil redisUtil,
      LockUtil lockUtil,
      GasSponsorAttemptRecordMapper recordMapper,
      HttpServletRequest request) {
    this.properties = properties;
    this.redisUtil = redisUtil;
    this.lockUtil = lockUtil;
    this.recordMapper = recordMapper;
    this.request = request;
  }

  public <T> T execute(
      PaymentOrder order,
      String providerId,
      String requestType,
      String walletAddress,
      Supplier<T> action) {
    String lockKey = RedisKeyNamespace.gasSponsorOrderLock(properties, order.getCryptoOrderNo());
    return lockUtil.withLock(
        lockKey,
        1000,
        Math.max(5, properties.getGas().getSponsorOrderLockLeaseSeconds()),
        () -> {
          if (StringUtils.hasText(order.getPaymentTxHash())) {
            record(order, providerId, requestType, walletAddress, "REJECTED", null, "order already has payment tx hash");
            throw new IllegalStateException("Order already has payment tx hash");
          }
          verifyQuota(order, providerId, requestType, walletAddress);
          try {
            T result = action.get();
            record(order, providerId, requestType, walletAddress, "SUCCESS", extractTxHash(result), null);
            return result;
          } catch (RuntimeException ex) {
            record(order, providerId, requestType, walletAddress, "FAILED", null, trim(ex.getMessage(), 512));
            throw ex;
          }
        });
  }

  private void verifyQuota(PaymentOrder order, String providerId, String requestType, String walletAddress) {
    if (!properties.getGas().isSponsorProtectionEnabled()) {
      return;
    }
    int ttl = Math.max(60, properties.getGas().getSponsorCounterTtlSeconds());
    verifyCounter(
        key("order", providerId, requestType, order.getCryptoOrderNo()),
        properties.getGas().getSponsorOrderMaxAttempts(),
        ttl,
        order,
        providerId,
        requestType,
        walletAddress,
        "order sponsor attempt limit exceeded");
    if (StringUtils.hasText(walletAddress)) {
      verifyCounter(
          key("wallet", providerId, requestType, normalize(walletAddress)),
          properties.getGas().getSponsorWalletMaxAttempts(),
          ttl,
          order,
          providerId,
          requestType,
          walletAddress,
          "wallet sponsor attempt limit exceeded");
    }
    String ipAddress = clientIp();
    if (StringUtils.hasText(ipAddress)) {
      verifyCounter(
          key("ip", providerId, requestType, ipAddress),
          properties.getGas().getSponsorIpMaxAttempts(),
          ttl,
          order,
          providerId,
          requestType,
          walletAddress,
          "ip sponsor attempt limit exceeded");
    }
    String cashierToken = cashierToken();
    if (StringUtils.hasText(cashierToken)) {
      verifyCounter(
          key("cashier-token", providerId, requestType, sha256Hex(cashierToken)),
          properties.getGas().getSponsorCashierTokenMaxAttempts(),
          ttl,
          order,
          providerId,
          requestType,
          walletAddress,
          "cashier token sponsor attempt limit exceeded");
    }
  }

  private void verifyCounter(
      String key,
      int maxAttempts,
      int ttlSeconds,
      PaymentOrder order,
      String providerId,
      String requestType,
      String walletAddress,
      String message) {
    if (maxAttempts <= 0) {
      return;
    }
    long count = redisUtil.incrementAndExpire(key, ttlSeconds, TimeUnit.SECONDS);
    if (count > maxAttempts) {
      record(order, providerId, requestType, walletAddress, "REJECTED", null, message);
      throw new IllegalStateException(message);
    }
  }

  private void record(
      PaymentOrder order,
      String providerId,
      String requestType,
      String walletAddress,
      String status,
      String txHash,
      String rejectReason) {
    GasSponsorAttemptRecord record = new GasSponsorAttemptRecord();
    record.setCryptoOrderNo(order.getCryptoOrderNo());
    record.setMerchantId(order.getMerchantId());
    record.setChain(order.getChain());
    record.setToken(order.getToken());
    record.setTokenAddress(order.getTokenAddress());
    record.setWalletAddress(StringUtils.hasText(walletAddress) ? walletAddress : order.getWalletAddress());
    record.setIpAddress(clientIp());
    record.setCashierTokenHash(StringUtils.hasText(cashierToken()) ? sha256Hex(cashierToken()) : null);
    record.setProviderId(providerId);
    record.setRequestType(requestType);
    record.setStatus(status);
    record.setTxHash(txHash);
    record.setRejectReason(trim(rejectReason, 512));
    record.setCreatedAt(LocalDateTime.now());
    record.setUpdatedAt(record.getCreatedAt());
    recordMapper.insert(record);
  }

  private String extractTxHash(Object result) {
    if (result instanceof GasSponsorExecutionResult executionResult) {
      return executionResult.txHash();
    }
    return null;
  }

  private String key(String dimension, String providerId, String requestType, String value) {
    return RedisKeyNamespace.gasSponsorQuota(properties, providerId, requestType, dimension, value);
  }

  private String clientIp() {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (StringUtils.hasText(forwarded)) {
      return forwarded.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    return StringUtils.hasText(realIp) ? realIp.trim() : request.getRemoteAddr();
  }

  private String cashierToken() {
    Object attr = request.getAttribute("cashierToken");
    if (attr instanceof String token && StringUtils.hasText(token)) {
      return token;
    }
    String header = request.getHeader("X-Cashier-Token");
    return StringUtils.hasText(header) ? header : null;
  }

  private String sha256Hex(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to hash cashier token", ex);
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String trim(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
