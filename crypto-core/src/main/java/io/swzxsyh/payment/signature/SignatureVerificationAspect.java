package io.swzxsyh.payment.signature;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.JsonUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SignatureVerificationAspect {

  private final CryptoPaymentProperties properties;
  private final SignatureKeyService keyService;
  private final SignatureReplayService replayService;
  private final HttpServletRequest request;

  public SignatureVerificationAspect(
      CryptoPaymentProperties properties,
      SignatureKeyService keyService,
      SignatureReplayService replayService,
      HttpServletRequest request) {
    this.properties = properties;
    this.keyService = keyService;
    this.replayService = replayService;
    this.request = request;
  }

  @Around("@annotation(io.swzxsyh.payment.signature.SignatureVerified)")
  public Object verify(ProceedingJoinPoint joinPoint) throws Throwable {
    if (!properties.getSignature().isEnabled() || !properties.getSignature().isVerifyInboundEnabled()) {
      return joinPoint.proceed();
    }

    SignatureVerified signatureVerified = resolveSignatureVerified(joinPoint);
    String merchantId = resolveMerchantId(joinPoint.getArgs());
    if (signatureVerified.merchantRequired() && !StringUtils.hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required");
    }

    String timestamp = header(properties.getSignature().getTimestampHeader());
    String nonce = header(properties.getSignature().getNonceHeader());
    String signatureValue = header(properties.getSignature().getSignatureHeader());
    String keyVersion = header(properties.getSignature().getKeyVersionHeader());

    validateTimestamp(timestamp);
    String payload = canonicalPayload(joinPoint.getArgs());
    String canonical = buildCanonical(signatureVerified.scope().name(), merchantId, timestamp, nonce, payload);

    if (!StringUtils.hasText(signatureValue)) {
      throw new IllegalArgumentException("signature is required");
    }

    MerchantSignatureKeyHolder merchantKey = resolveMerchantKey(merchantId, keyVersion);
    boolean verified = SignatureUtil.verifyRsaSha256(canonical, signatureValue, merchantKey.publicKeyPem());
    if (!verified) {
      throw new IllegalArgumentException("signature verification failed");
    }

    replayService.claim(signatureVerified.scope().name(), merchantId, nonce, canonical,
        properties.getSignature().getReplayTtlSeconds());
    return joinPoint.proceed();
  }

  private SignatureVerified resolveSignatureVerified(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    SignatureVerified annotation = method.getAnnotation(SignatureVerified.class);
    if (annotation == null) {
      throw new IllegalStateException("SignatureVerified annotation is required");
    }
    return annotation;
  }

  private MerchantSignatureKeyHolder resolveMerchantKey(String merchantId, String keyVersion) {
    var key = keyService.resolveMerchantKey(merchantId, keyVersion);
    return new MerchantSignatureKeyHolder(key.getPublicKeyPem());
  }

  private String resolveMerchantId(Object[] args) {
    String merchantId = attribute("merchantId");
    if (StringUtils.hasText(merchantId)) {
      return merchantId;
    }
    merchantId = header(properties.getSignature().getMerchantIdHeader());
    if (StringUtils.hasText(merchantId)) {
      return merchantId;
    }
    for (Object arg : args) {
      if (arg == null || isInfrastructureArg(arg)) {
        continue;
      }
      try {
        var tree = JsonUtil.toTree(arg);
        if (tree != null && tree.hasNonNull("merchantId")) {
          String value = tree.get("merchantId").asText();
          if (StringUtils.hasText(value)) {
            return value;
          }
        }
      } catch (Exception ignored) {
        // ignore and continue
      }
    }
    return null;
  }

  private String attribute(String name) {
    Object value = request.getAttribute(name);
    return value == null ? null : String.valueOf(value);
  }

  private boolean isInfrastructureArg(Object arg) {
    return arg instanceof HttpServletRequest
        || arg instanceof jakarta.servlet.http.HttpServletResponse
        || arg instanceof org.springframework.validation.BindingResult;
  }

  private String canonicalPayload(Object[] args) {
    List<Object> payloadArgs = new ArrayList<>();
    for (Object arg : args) {
      if (arg == null || isInfrastructureArg(arg)) {
        continue;
      }
      payloadArgs.add(arg);
    }
    return JsonUtil.toJson(payloadArgs);
  }

  private String buildCanonical(String scope, String merchantId, String timestamp, String nonce, String payload) {
    return scope + "\n"
        + request.getMethod() + "\n"
        + request.getRequestURI() + "\n"
        + (merchantId == null ? "" : merchantId) + "\n"
        + timestamp + "\n"
        + nonce + "\n"
        + payload;
  }

  private void validateTimestamp(String timestamp) {
    if (!StringUtils.hasText(timestamp)) {
      throw new IllegalArgumentException("timestamp is required");
    }
    long ts;
    try {
      ts = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("timestamp must be epoch millis");
    }
    long now = Instant.now().toEpochMilli();
    long skew = Math.abs(now - ts);
    long allowed = properties.getSignature().getAllowedClockSkewSeconds() * 1000L;
    if (skew > allowed) {
      throw new IllegalArgumentException("signature timestamp expired");
    }
  }

  private String header(String name) {
    return request.getHeader(name);
  }

  private record MerchantSignatureKeyHolder(String publicKeyPem) {
  }
}
