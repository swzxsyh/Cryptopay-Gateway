package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.HttpClientUtil;
import io.swzxsyh.payment.util.JsonUtil;
import io.swzxsyh.payment.util.SecretValueResolver;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 第三方托管钱包 API 策略。 */
@Slf4j
@Component
public class ThirdPartyWalletApiStrategy implements DerivedAddressStrategy {

  private final CryptoPaymentProperties properties;
  private final SecretValueResolver secretValueResolver;
  private final HttpClientUtil httpClientUtil;

  public ThirdPartyWalletApiStrategy(
      CryptoPaymentProperties properties,
      SecretValueResolver secretValueResolver,
      HttpClientUtil httpClientUtil) {
    this.properties = properties;
    this.secretValueResolver = secretValueResolver;
    this.httpClientUtil = httpClientUtil;
  }

  @Override
  public DerivedAddressProvisionerMode mode() {
    return DerivedAddressProvisionerMode.THIRD_PARTY_API;
  }

  @Override
  public List<String> createAddresses(String chain, String token, int count, String orderNo) {
    CryptoPaymentProperties.DerivedAddress.ThirdPartyApi config = properties.getDerivedAddress().getThirdPartyApi();
    if (!config.isEnabled()) {
      throw new IllegalStateException("Third party wallet API strategy is disabled");
    }
    if (!StringUtils.hasText(config.getBaseUrl())) {
      throw new IllegalStateException("Third party wallet API baseUrl is required");
    }

    try {
      Map<String, Object> request = Map.of(
          "chain", chain,
          "token", token,
          "count", count,
          "orderNo", orderNo,
          "providerName", config.getProviderName()
      );
      Map<String, String> headers = new LinkedHashMap<>();
      String apiKey = secretValueResolver.resolveOptional(
          config.getApiKeySourceType(),
          config.getApiKey(),
          config.getApiKeyEnv(),
          config.getApiKeyKmsKeyId(),
          "CRYPTO_PAYMENT_DERIVED_THIRD_PARTY_API_KEY");
      if (StringUtils.hasText(apiKey)) {
        headers.put("Authorization", "Bearer " + apiKey);
      }

      String url = normalizeUrl(config.getBaseUrl(), config.getCreatePath());
      HttpClientUtil.ExternalHttpResponse response = httpClientUtil.postJson(url, request, headers);
      if (!response.is2xxSuccessful()) {
        throw new IllegalStateException("Third party wallet API returned non-2xx status: " + response.statusCode());
      }
      String responseBody = response.body();
      if (!StringUtils.hasText(responseBody)) {
        throw new IllegalStateException("Third party wallet API returned empty response");
      }

      JsonNode root = JsonUtil.readTree(responseBody);
      List<String> addresses = new ArrayList<>(count);
      if (root.isArray()) {
        for (JsonNode node : root) {
          if (node.isTextual()) {
            addresses.add(node.asText());
          }
        }
      } else if (root.has("addresses") && root.get("addresses").isArray()) {
        for (JsonNode node : root.get("addresses")) {
          if (node.isTextual()) {
            addresses.add(node.asText());
          }
        }
      }
      if (addresses.isEmpty()) {
        throw new IllegalStateException("Third party wallet API did not return addresses");
      }

      log.info("Fetched addresses from third party wallet API. provider={}, count={}, orderNo={}",
          config.getProviderName(), addresses.size(), orderNo);
      return addresses;
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to create derived addresses from third party wallet API", ex);
    }
  }

  private String normalizeUrl(String baseUrl, String path) {
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    String suffix = path.startsWith("/") ? path : "/" + path;
    return base + suffix;
  }
}
