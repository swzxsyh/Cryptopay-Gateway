package io.swzxsyh.payment.security;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 回调地址与跳转地址安全校验服务，防止非法重定向和本地地址注入。 */
@Service
public class UrlSafetyService {

  private final CryptoPaymentProperties properties;

  public UrlSafetyService(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  /** 校验商户回调地址。 */
  public void validateMerchantCallbackUrl(String url, String fieldName) {
    validateHttpUrl(url, fieldName);
  }

  /** 校验商户返回地址。 */
  public void validateMerchantReturnUrl(String url, String fieldName) {
    if (!StringUtils.hasText(url)) {
      return;
    }
    if (!properties.getSecurity().isValidateRedirectUrl()) {
      return;
    }
    validateHttpUrl(url, fieldName);
    if (!isHostAllowed(url)) {
      throw new IllegalArgumentException(fieldName + " host is not allowed");
    }
  }

  /** 校验 URL 的协议、主机和本地回环限制。 */
  private void validateHttpUrl(String url, String fieldName) {
    if (!StringUtils.hasText(url)) {
      throw new IllegalArgumentException(fieldName + " is required");
    }
    URI uri;
    try {
      uri = URI.create(url.trim());
    } catch (Exception e) {
      throw new IllegalArgumentException(fieldName + " must be a valid URL");
    }
    String scheme = uri.getScheme();
    if (!StringUtils.hasText(scheme)) {
      throw new IllegalArgumentException(fieldName + " must include scheme");
    }
    String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
    if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
      throw new IllegalArgumentException(fieldName + " must use http or https");
    }
    if (!StringUtils.hasText(uri.getHost())) {
      throw new IllegalArgumentException(fieldName + " must include host");
    }
    if (!properties.getSecurity().isAllowLocalRedirect() && isLocalHost(uri.getHost())) {
      throw new IllegalArgumentException(fieldName + " cannot point to local host");
    }
  }

  /** 判断跳转地址是否属于允许的 host 白名单。 */
  private boolean isHostAllowed(String url) {
    URI uri = URI.create(url.trim());
    List<String> allowedHosts = properties.getSecurity().getAllowedRedirectHosts();
    if (allowedHosts == null || allowedHosts.isEmpty()) {
      return true;
    }
    String host = uri.getHost();
    if (!StringUtils.hasText(host)) {
      return false;
    }
    String normalized = host.toLowerCase(Locale.ROOT);
    return allowedHosts.stream()
        .filter(StringUtils::hasText)
        .map(value -> value.toLowerCase(Locale.ROOT))
        .anyMatch(allowed -> allowed.equals(normalized) || normalized.endsWith("." + allowed));
  }

  /** 判断是否为本机或本地回环地址。 */
  private boolean isLocalHost(String host) {
    String normalized = host.toLowerCase(Locale.ROOT);
    return "localhost".equals(normalized)
        || "127.0.0.1".equals(normalized)
        || "::1".equals(normalized);
  }
}
