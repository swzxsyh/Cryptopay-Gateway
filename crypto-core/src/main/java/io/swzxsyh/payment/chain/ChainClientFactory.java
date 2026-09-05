package io.swzxsyh.payment.chain;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.util.HttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 按链配置创建并缓存链客户端。 */
@Component
public class ChainClientFactory {

  private final Map<String, ChainClient> cache = new ConcurrentHashMap<>();
  private final CryptoPaymentProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClientUtil httpClientUtil;

  public ChainClientFactory(
      CryptoPaymentProperties properties,
      ObjectMapper objectMapper,
      HttpClientUtil httpClientUtil) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClientUtil = httpClientUtil;
  }

  public ChainClient get(String chain) {
    return cache.computeIfAbsent(normalize(chain), this::createClient);
  }

  private ChainClient createClient(String chain) {
    CryptoPaymentProperties.ChainProfile profile = properties.getChainProfiles().stream()
        .filter(item -> item.isEnabled() && chain.equalsIgnoreCase(item.getChain()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("No chain profile configured for chain: " + chain));
    ChainFamily family = ChainFamilyResolver.resolve(profile.getChain());
    return switch (family) {
      case TRON, SUI, TON -> new UnsupportedChainClient(family);
      case SOLANA -> new SolanaChainClient(profile.getRpcUrl(), objectMapper, httpClientUtil);
      case EVM, UNKNOWN -> new Web3jChainClient(profile.getRpcUrl());
    };
  }

  private String normalize(String chain) {
    if (!StringUtils.hasText(chain)) {
      throw new IllegalArgumentException("chain is required");
    }
    return chain.toUpperCase();
  }
}
