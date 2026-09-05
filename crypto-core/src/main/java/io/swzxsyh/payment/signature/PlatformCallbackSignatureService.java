package io.swzxsyh.payment.signature;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.persistence.entity.PlatformSigningKey;
import io.swzxsyh.payment.util.JsonUtil;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

/** 平台回调签名服务。 */
@Slf4j
@Service
public class PlatformCallbackSignatureService {

  private final CryptoPaymentProperties properties;
  private final SignatureKeyService signatureKeyService;

  public PlatformCallbackSignatureService(
      CryptoPaymentProperties properties, SignatureKeyService signatureKeyService) {
    this.properties = properties;
    this.signatureKeyService = signatureKeyService;
  }

  public <T> HttpEntity<T> sign(String merchantId, T body) {
    if (!properties.getSignature().isEnabled()
        || !properties.getSignature().isSignOutboundEnabled()) {
      return new HttpEntity<>(body);
    }

    PlatformSigningKey key = signatureKeyService.resolveActivePlatformKey();
    String payload = toJson(body);
    String timestamp = String.valueOf(Instant.now().toEpochMilli());
    String nonce = UUID.randomUUID().toString().replace("-", "");
    String canonical = buildCanonical("CALLBACK_SIGN", merchantId, timestamp, nonce, payload);
    String signature = SignatureUtil.signRsaSha256(canonical, key.getPrivateKeyPem());

    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(merchantId)) {
      headers.add(properties.getSignature().getMerchantIdHeader(), merchantId);
    }
    headers.add(properties.getSignature().getTimestampHeader(), timestamp);
    headers.add(properties.getSignature().getNonceHeader(), nonce);
    headers.add(properties.getSignature().getSignatureHeader(), signature);
    headers.add(properties.getSignature().getKeyVersionHeader(), key.getKeyAlias());
    headers.add("X-Sign-Alg", key.getAlgorithm());
    return new HttpEntity<>(body, headers);
  }

  /**
   * Signs an already serialized JSON payload.
   *
   * <p>Callbacks use this method so the canonical payload used for signing is the same JSON string
   * that is sent on the wire. This avoids verification failures caused by different serializers or
   * field ordering between signing and HTTP transmission.
   */
  public HttpEntity<String> signJson(String merchantId, String payloadJson) {
    if (!properties.getSignature().isEnabled()
        || !properties.getSignature().isSignOutboundEnabled()) {
      return new HttpEntity<>(payloadJson);
    }

    PlatformSigningKey key = signatureKeyService.resolveActivePlatformKey();
    String payload = payloadJson == null ? "" : payloadJson;
    String timestamp = String.valueOf(Instant.now().toEpochMilli());
    String nonce = UUID.randomUUID().toString().replace("-", "");
    String canonical = buildCanonical("CALLBACK_SIGN", merchantId, timestamp, nonce, payload);
    String signature = SignatureUtil.signRsaSha256(canonical, key.getPrivateKeyPem());

    HttpHeaders headers = new HttpHeaders();
    if (StringUtils.hasText(merchantId)) {
      headers.add(properties.getSignature().getMerchantIdHeader(), merchantId);
    }
    headers.add(properties.getSignature().getTimestampHeader(), timestamp);
    headers.add(properties.getSignature().getNonceHeader(), nonce);
    headers.add(properties.getSignature().getSignatureHeader(), signature);
    headers.add(properties.getSignature().getKeyVersionHeader(), key.getKeyAlias());
    headers.add("X-Sign-Alg", key.getAlgorithm());
    return new HttpEntity<>(payload, headers);
  }

  private String buildCanonical(
      String scope, String merchantId, String timestamp, String nonce, String payload) {
    return scope
        + "\n"
        + (merchantId == null ? "" : merchantId)
        + "\n"
        + timestamp
        + "\n"
        + nonce
        + "\n"
        + payload;
  }

  private String toJson(Object value) {
    return JsonUtil.toJson(value);
  }
}
