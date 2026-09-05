package io.swzxsyh.payment.util;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 基于 HTTP 的通用 KMS 密钥读取器。 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "crypto.payment.secrets.kms", name = "enabled", havingValue = "true")
public class HttpSecretKmsClient implements SecretKmsClient {

  private final CryptoPaymentProperties properties;
  private final HttpClientUtil httpClientUtil;

  public HttpSecretKmsClient(CryptoPaymentProperties properties, HttpClientUtil httpClientUtil) {
    this.properties = properties;
    this.httpClientUtil = httpClientUtil;
  }

  @Override
  public String resolve(String keyId) {
    if (!StringUtils.hasText(keyId)) {
      throw new IllegalArgumentException("kms keyId is required");
    }
    CryptoPaymentProperties.Kms kms = properties.getSecrets().getKms();
    if (kms == null || !StringUtils.hasText(kms.getBaseUrl())) {
      throw new IllegalStateException("crypto.payment.secrets.kms.base-url is required");
    }

    String url = normalizeUrl(kms.getBaseUrl(), kms.getResolvePath(), keyId);
    Map<String, String> headers = new LinkedHashMap<>();
    String apiKey = resolveSecretValue(kms.getApiKeyEnv(), kms.getApiKey());
    if (StringUtils.hasText(apiKey)) {
      headers.put(kms.getApiKeyHeader(), apiKey);
    }

    try {
      HttpClientUtil.ExternalHttpResponse response =
          httpClientUtil.postJson(url, Map.of("keyId", keyId), headers);
      if (!response.is2xxSuccessful()) {
        throw new IllegalStateException("KMS returned non-2xx status: " + response.statusCode());
      }
      String responseBody = response.body();
      if (!StringUtils.hasText(responseBody)) {
        throw new IllegalStateException("KMS returned empty response");
      }

      String responseField = StringUtils.hasText(kms.getResponseField()) ? kms.getResponseField() : "value";
      String trimmed = responseBody.trim();
      if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        JsonNode root = JsonUtil.readTree(trimmed);
        if (root.isObject() && root.has(responseField) && root.get(responseField).isTextual()) {
          return root.get(responseField).asText();
        }
        if (root.isTextual()) {
          return root.asText();
        }
      }
      return trimmed;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to resolve secret from KMS: " + keyId, ex);
    }
  }

  private String normalizeUrl(String baseUrl, String resolvePath, String keyId) {
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String path = StringUtils.hasText(resolvePath) ? resolvePath : "/secrets/{keyId}";
    String encodedKeyId = URLEncoder.encode(keyId, StandardCharsets.UTF_8);
    String suffix = path.replace("{keyId}", encodedKeyId);
    if (!suffix.startsWith("/")) {
      suffix = "/" + suffix;
    }
    return base + suffix;
  }

  private String resolveSecretValue(String envName, String fallback) {
    if (StringUtils.hasText(envName)) {
      String value = System.getenv(envName);
      if (!StringUtils.hasText(value)) {
        value = System.getProperty(envName);
      }
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return fallback;
  }
}
