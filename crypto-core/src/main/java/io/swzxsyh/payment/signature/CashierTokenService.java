package io.swzxsyh.payment.signature;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.mapper.CashierTokenMappingMapper;
import io.swzxsyh.payment.mapper.PlatformCipherKeyMapper;
import io.swzxsyh.payment.persistence.entity.CashierTokenMapping;
import io.swzxsyh.payment.persistence.entity.PlatformCipherKey;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.util.JsonUtil;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 收银台 token 的加密、解密与解析服务。 */
@Slf4j
@Service
public class CashierTokenService {

  private static final String TOKEN_VERSION = "v1";
  private static final String BIZ_TYPE_CRYPTO_ORDER = "CRYPTO_ORDER";
  private static final String BIZ_TYPE_SUBSCRIPTION_ORDER = "SUBSCRIPTION_ORDER";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int KEY_LENGTH = 32;
  private static final char[] SHORT_CODE_CHARS =
      "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
  private static final int MAX_SHORT_CODE_ATTEMPTS = 8;

  private final CryptoPaymentProperties properties;
  private final PlatformCipherKeyMapper keyMapper;
  private final CashierTokenMappingMapper tokenMappingMapper;
  private final SecureRandom secureRandom = new SecureRandom();

  public CashierTokenService(
      CryptoPaymentProperties properties,
      PlatformCipherKeyMapper keyMapper,
      CashierTokenMappingMapper tokenMappingMapper) {
    this.properties = properties;
    this.keyMapper = keyMapper;
    this.tokenMappingMapper = tokenMappingMapper;
  }

  /** 生成收银台 token。 */
  public String createToken(PaymentOrder order, Map<String, Object> attributes) {
    if (!properties.getCashier().isTokenEncryptionEnabled()) {
      return order.getCryptoOrderNo();
    }

    PlatformCipherKey key = resolveActiveKey();
    LocalDateTime now = LocalDateTime.now();
    CashierTokenPayload payload =
        new CashierTokenPayload(
            TOKEN_VERSION,
            key.getKeyAlias(),
            BIZ_TYPE_CRYPTO_ORDER,
            order.getCryptoOrderNo(),
            null,
            order.getMerchantId(),
            order.getMerchantOrderNo(),
            order.getAmount(),
            order.getCurrency(),
            order.getChain(),
            order.getToken(),
            now,
            now.plusMinutes(properties.getCashier().getTokenTtlMinutes()),
            attributes == null ? Map.of() : new LinkedHashMap<>(attributes));
    String encryptedToken = encrypt(payload, key);
    if (!properties.getCashier().isShortTokenEnabled()) {
      return encryptedToken;
    }
    return createShortToken(order.getCryptoOrderNo(), order.getMerchantId(), encryptedToken, payload.expireAt());
  }

  /** 生成订阅收银台 token，避免把订阅单号直接暴露在 URL 中。 */
  public String createSubscriptionToken(SubscriptionOrder order, Map<String, Object> attributes) {
    if (!properties.getCashier().isTokenEncryptionEnabled()) {
      log.warn("收银台 token 加密未开启，订阅收银台链接会退化为订阅单号。subscriptionOrderNo={}",
          order.getSubscriptionOrderNo());
      return order.getSubscriptionOrderNo();
    }

    PlatformCipherKey key = resolveActiveKey();
    LocalDateTime now = LocalDateTime.now();
    CashierTokenPayload payload =
        new CashierTokenPayload(
            TOKEN_VERSION,
            key.getKeyAlias(),
            BIZ_TYPE_SUBSCRIPTION_ORDER,
            null,
            order.getSubscriptionOrderNo(),
            order.getMerchantId(),
            order.getMerchantOrderNo(),
            order.getAmountPerCycle(),
            order.getCurrency(),
            order.getChain(),
            order.getToken(),
            now,
            now.plusMinutes(properties.getCashier().getTokenTtlMinutes()),
            attributes == null ? Map.of() : new LinkedHashMap<>(attributes));
    String encryptedToken = encrypt(payload, key);
    if (!properties.getCashier().isShortTokenEnabled()) {
      return encryptedToken;
    }
    return createShortToken(order.getSubscriptionOrderNo(), order.getMerchantId(), encryptedToken, payload.expireAt());
  }

  /** 解析收银台 token。 */
  public CashierTokenPayload resolveToken(String token) {
    if (!StringUtils.hasText(token)) {
      throw new IllegalArgumentException("cashier token is required");
    }
    String effectiveToken = resolveShortTokenIfNecessary(token.trim());
    if (!properties.getCashier().isTokenEncryptionEnabled()) {
      return new CashierTokenPayload(
          TOKEN_VERSION,
          properties.getCashier().getTokenKeyAlias(),
          null,
          effectiveToken,
          null,
          null,
          effectiveToken,
          null,
          null,
          null,
          null,
          LocalDateTime.now(),
          LocalDateTime.now(),
          Map.of());
    }

    String[] parts = effectiveToken.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("invalid cashier token");
    }

    String keyAlias = decodeSegment(parts[0]);
    PlatformCipherKey key = resolveKeyByAlias(keyAlias);
    byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
    byte[] cipherBytes = Base64.getUrlDecoder().decode(parts[2]);
    try {
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION);
      cipher.init(
          javax.crypto.Cipher.DECRYPT_MODE,
          keySpec(key),
          new javax.crypto.spec.GCMParameterSpec(128, iv));
      byte[] plain = cipher.doFinal(cipherBytes);
      return JsonUtil.fromJson(plain, CashierTokenPayload.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("failed to decrypt cashier token");
    }
  }

  /** 从收银台 token 中解析平台支付单号。 */
  public String resolveCryptoOrderNo(String token) {
    CashierTokenPayload payload = resolveToken(token);
    if (payload.expireAt() != null && LocalDateTime.now().isAfter(payload.expireAt())) {
      throw new IllegalArgumentException("cashier token expired");
    }
    if (StringUtils.hasText(payload.bizType()) && !BIZ_TYPE_CRYPTO_ORDER.equals(payload.bizType())) {
      throw new IllegalArgumentException("invalid cashier token business type");
    }
    return payload.cryptoOrderNo();
  }

  /** 从订阅收银台 token 中解析订阅单号。 */
  public String resolveSubscriptionOrderNo(String token) {
    CashierTokenPayload payload = resolveToken(token);
    if (payload.expireAt() != null && LocalDateTime.now().isAfter(payload.expireAt())) {
      throw new IllegalArgumentException("cashier token expired");
    }
    if (StringUtils.hasText(payload.bizType()) && !BIZ_TYPE_SUBSCRIPTION_ORDER.equals(payload.bizType())) {
      throw new IllegalArgumentException("invalid subscription cashier token business type");
    }
    String subscriptionOrderNo = payload.subscriptionOrderNo();
    if (!StringUtils.hasText(subscriptionOrderNo) && !StringUtils.hasText(payload.bizType())) {
      subscriptionOrderNo = payload.cryptoOrderNo();
    }
    if (!StringUtils.hasText(subscriptionOrderNo)) {
      throw new IllegalArgumentException("subscription order not found in cashier token");
    }
    return subscriptionOrderNo;
  }

  private String encrypt(CashierTokenPayload payload, PlatformCipherKey key) {
    try {
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);
      javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance(TRANSFORMATION);
      cipher.init(
          javax.crypto.Cipher.ENCRYPT_MODE,
          keySpec(key),
          new javax.crypto.spec.GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(JsonUtil.toBytes(payload));
      return encodeSegment(key.getKeyAlias())
          + "."
          + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
          + "."
          + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
    } catch (Exception e) {
      throw new IllegalStateException("failed to encrypt cashier token", e);
    }
  }

  private String createShortToken(
      String bizOrderNo, String merchantId, String encryptedToken, LocalDateTime expireAt) {
    for (int attempt = 1; attempt <= MAX_SHORT_CODE_ATTEMPTS; attempt++) {
      String shortCode = buildShortCode();
      CashierTokenMapping mapping = new CashierTokenMapping();
      LocalDateTime now = LocalDateTime.now();
      mapping.setShortCode(shortCode);
      mapping.setEncryptedToken(encryptedToken);
      mapping.setCryptoOrderNo(bizOrderNo);
      mapping.setMerchantId(merchantId);
      mapping.setExpireAt(expireAt);
      mapping.setCreatedAt(now);
      mapping.setUpdatedAt(now);
      try {
        tokenMappingMapper.insert(mapping);
        log.info("Created cashier short token. bizOrderNo={}, shortCode={}", bizOrderNo, shortCode);
        return shortCode;
      } catch (DuplicateKeyException ex) {
        log.debug("Cashier short token collided, retrying. attempt={}, shortCode={}", attempt, shortCode);
      }
    }
    throw new IllegalStateException("failed to create unique cashier short token");
  }

  private String resolveShortTokenIfNecessary(String token) {
    if (!properties.getCashier().isShortTokenEnabled() || token.contains(".")) {
      return token;
    }
    CashierTokenMapping mapping =
        tokenMappingMapper.selectOne(
            new LambdaQueryWrapper<CashierTokenMapping>()
                .eq(CashierTokenMapping::getShortCode, token)
                .last("LIMIT 1"));
    if (mapping == null) {
      return token;
    }
    if (mapping.getExpireAt() != null && LocalDateTime.now().isAfter(mapping.getExpireAt())) {
      throw new IllegalArgumentException("cashier token expired");
    }
    if (!StringUtils.hasText(mapping.getEncryptedToken())) {
      throw new IllegalArgumentException("cashier token mapping is empty");
    }
    return mapping.getEncryptedToken();
  }

  private String buildShortCode() {
    int length = Math.max(8, properties.getCashier().getShortTokenLength());
    StringBuilder builder = new StringBuilder(length + 4);
    String prefix = properties.getCashier().getShortTokenPrefix();
    if (StringUtils.hasText(prefix)) {
      builder.append(prefix.trim()).append('_');
    }
    for (int i = 0; i < length; i++) {
      builder.append(SHORT_CODE_CHARS[secureRandom.nextInt(SHORT_CODE_CHARS.length)]);
    }
    return builder.toString();
  }

  /** 定期清理过期短码，避免映射表长期膨胀。 */
  @Scheduled(cron = "0 10 3 * * ?")
  public void purgeExpiredShortTokens() {
    int removed =
        tokenMappingMapper.delete(
            new LambdaQueryWrapper<CashierTokenMapping>()
                .lt(CashierTokenMapping::getExpireAt, LocalDateTime.now()));
    if (removed > 0) {
      log.info("Purged expired cashier short tokens. removed={}", removed);
    }
  }

  private PlatformCipherKey resolveActiveKey() {
    String alias = properties.getCashier().getTokenKeyAlias();
    PlatformCipherKey key =
        keyMapper.selectOne(
            new LambdaQueryWrapper<PlatformCipherKey>()
                .eq(PlatformCipherKey::getKeyAlias, alias)
                .last("LIMIT 1"));
    if (key == null) {
      key = new PlatformCipherKey();
      key.setKeyAlias(alias);
      key.setAlgorithm("AES_GCM");
      key.setSecretKeyBase64(generateSecretKey());
      key.setActive(true);
      key.setCreatedAt(LocalDateTime.now());
      key.setUpdatedAt(LocalDateTime.now());
      keyMapper.insert(key);
      return key;
    }

    boolean changed = false;
    if (!StringUtils.hasText(key.getSecretKeyBase64())) {
      key.setSecretKeyBase64(generateSecretKey());
      changed = true;
    }
    if (!key.isActive()) {
      key.setActive(true);
      changed = true;
    }
    if (changed) {
      key.setUpdatedAt(LocalDateTime.now());
      keyMapper.updateById(key);
    }
    return key;
  }

  private PlatformCipherKey resolveKeyByAlias(String alias) {
    PlatformCipherKey key =
        keyMapper.selectOne(
            new LambdaQueryWrapper<PlatformCipherKey>()
                .eq(PlatformCipherKey::getKeyAlias, alias)
                .eq(PlatformCipherKey::isActive, true)
                .last("LIMIT 1"));
    if (key == null || !StringUtils.hasText(key.getSecretKeyBase64())) {
      throw new IllegalArgumentException("cashier key not found");
    }
    return key;
  }

  private javax.crypto.SecretKey keySpec(PlatformCipherKey key) {
    byte[] bytes = Base64.getDecoder().decode(key.getSecretKeyBase64());
    return new javax.crypto.spec.SecretKeySpec(bytes, "AES");
  }

  private String generateSecretKey() {
    byte[] bytes = new byte[KEY_LENGTH];
    secureRandom.nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  private String encodeSegment(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private String decodeSegment(String value) {
    return new String(
        Base64.getUrlDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
  }
}
