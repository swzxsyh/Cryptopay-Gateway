package io.swzxsyh.payment.signature;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.mapper.MerchantSignatureKeyMapper;
import io.swzxsyh.payment.mapper.PlatformSigningKeyMapper;
import io.swzxsyh.payment.persistence.entity.MerchantSignatureKey;
import io.swzxsyh.payment.persistence.entity.PlatformSigningKey;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 平台与商户签名密钥管理服务。 */
@Slf4j
@Service
public class SignatureKeyService {

  private final MerchantSignatureKeyMapper merchantKeyMapper;
  private final PlatformSigningKeyMapper platformKeyMapper;
  private final CryptoPaymentProperties properties;

  public SignatureKeyService(
      MerchantSignatureKeyMapper merchantKeyMapper,
      PlatformSigningKeyMapper platformKeyMapper,
      CryptoPaymentProperties properties) {
    this.merchantKeyMapper = merchantKeyMapper;
    this.platformKeyMapper = platformKeyMapper;
    this.properties = properties;
  }

  public MerchantSignatureKey resolveMerchantKey(String merchantId, String keyVersion) {
    if (!StringUtils.hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required for signature verification");
    }

    LambdaQueryWrapper<MerchantSignatureKey> query =
        new LambdaQueryWrapper<MerchantSignatureKey>()
            .eq(MerchantSignatureKey::getMerchantId, merchantId)
            .eq(MerchantSignatureKey::isEnabled, true)
            .orderByDesc(MerchantSignatureKey::getUpdatedAt)
            .last(StringUtils.hasText(keyVersion) ? "LIMIT 1" : "LIMIT 1");

    if (StringUtils.hasText(keyVersion)) {
      query.eq(MerchantSignatureKey::getKeyVersion, keyVersion);
    }

    MerchantSignatureKey key = merchantKeyMapper.selectOne(query);
    if (key == null || !StringUtils.hasText(key.getPublicKeyPem())) {
      throw new IllegalStateException("merchant signature key not found: " + merchantId);
    }
    return key;
  }

  public PlatformSigningKey resolveActivePlatformKey() {
    if (!properties.getSignature().isEnabled()) {
      throw new IllegalStateException("signature module is disabled");
    }
    PlatformSigningKey key =
        platformKeyMapper.selectOne(
            new LambdaQueryWrapper<PlatformSigningKey>()
                .eq(PlatformSigningKey::isActive, true)
                .orderByDesc(PlatformSigningKey::getUpdatedAt)
                .last("LIMIT 1"));
    if (key == null || !StringUtils.hasText(key.getPrivateKeyPem())) {
      throw new IllegalStateException("active platform signing key not found");
    }
    return key;
  }

  public Optional<MerchantSignatureKey> findMerchantKey(String merchantId) {
    if (!StringUtils.hasText(merchantId)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        merchantKeyMapper.selectOne(
            new LambdaQueryWrapper<MerchantSignatureKey>()
                .eq(MerchantSignatureKey::getMerchantId, merchantId)
                .eq(MerchantSignatureKey::isEnabled, true)
                .orderByDesc(MerchantSignatureKey::getUpdatedAt)
                .last("LIMIT 1")));
  }

  public PlatformSigningKey savePlatformKey(PlatformSigningKey key) {
    if (key.getCreatedAt() == null) {
      key.setCreatedAt(LocalDateTime.now());
    }
    key.setUpdatedAt(LocalDateTime.now());
    if (key.getId() == null) {
      platformKeyMapper.insert(key);
    } else {
      platformKeyMapper.updateById(key);
    }
    return key;
  }
}
