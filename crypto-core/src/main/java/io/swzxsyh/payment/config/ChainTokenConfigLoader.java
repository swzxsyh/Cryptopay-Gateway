package io.swzxsyh.payment.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.PaymentChainConfigMapper;
import io.swzxsyh.payment.mapper.PaymentTokenCapabilityMapper;
import io.swzxsyh.payment.mapper.PaymentTokenConfigMapper;
import io.swzxsyh.payment.persistence.entity.PaymentChainConfig;
import io.swzxsyh.payment.persistence.entity.PaymentTokenCapability;
import io.swzxsyh.payment.persistence.entity.PaymentTokenConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 链、币种合约地址和币种协议能力配置加载器。 */
@Component
@Order(130)
public class ChainTokenConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentChainConfigMapper chainMapper;
  private final PaymentTokenConfigMapper tokenMapper;
  private final PaymentTokenCapabilityMapper tokenCapabilityMapper;

  public ChainTokenConfigLoader(
      CryptoPaymentProperties properties,
      PaymentChainConfigMapper chainMapper,
      PaymentTokenConfigMapper tokenMapper,
      PaymentTokenCapabilityMapper tokenCapabilityMapper) {
    this.properties = properties;
    this.chainMapper = chainMapper;
    this.tokenMapper = tokenMapper;
    this.tokenCapabilityMapper = tokenCapabilityMapper;
  }

  @Override
  public void apply() {
    applyChains();
    applyTokens();
  }

  private void applyChains() {
    properties.setChainProfiles(
        chainMapper.selectList(Wrappers.<PaymentChainConfig>lambdaQuery()
                .eq(PaymentChainConfig::getEnabled, Boolean.TRUE)
                .orderByAsc(PaymentChainConfig::getSortNo))
            .stream()
            .map(config -> {
              CryptoPaymentProperties.ChainProfile profile = new CryptoPaymentProperties.ChainProfile();
              profile.setChain(config.getChainCode());
              profile.setRpcUrl(config.getRpcUrl());
              profile.setWsUrl(config.getWsUrl());
              profile.setConfirmationDepth(config.getConfirmationDepth());
              profile.setSponsorEnabled(config.getSponsorEnabled());
              profile.setSponsorProvider(config.getSponsorProvider());
              profile.setRelayerAddress(config.getRelayerAddress());
              profile.setSponsorGasLimit(config.getSponsorGasLimit());
              profile.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
              return profile;
            })
            .toList());
  }

  private void applyTokens() {
    Set<String> enabledChains = chainMapper.selectList(Wrappers.<PaymentChainConfig>lambdaQuery()
            .eq(PaymentChainConfig::getEnabled, Boolean.TRUE))
        .stream()
        .map(config -> normalize(config.getChainCode()))
        .collect(java.util.stream.Collectors.toSet());
    Map<String, PaymentTokenCapability> capabilityMap = tokenCapabilityMapper
        .selectList(Wrappers.<PaymentTokenCapability>lambdaQuery()
            .eq(PaymentTokenCapability::getEnabled, Boolean.TRUE))
        .stream()
        .collect(LinkedHashMap::new, (map, capability) ->
                map.put(capabilityKey(capability.getChainCode(), capability.getTokenSymbol()), capability),
            Map::putAll);

    properties.setTokenProfiles(
        tokenMapper.selectList(Wrappers.<PaymentTokenConfig>lambdaQuery()
                .eq(PaymentTokenConfig::getEnabled, Boolean.TRUE)
                .orderByAsc(PaymentTokenConfig::getSortNo))
            .stream()
            .filter(config -> enabledChains.contains(normalize(config.getChainCode())))
            .map(config -> {
              PaymentTokenCapability capability =
                  capabilityMap.get(capabilityKey(config.getChainCode(), config.getTokenSymbol()));
              CryptoPaymentProperties.TokenProfile profile = new CryptoPaymentProperties.TokenProfile();
              profile.setChain(config.getChainCode());
              profile.setToken(config.getTokenSymbol());
              profile.setTokenAddress(config.getTokenAddress());
              profile.setDecimals(config.getDecimals() == null ? 6 : config.getDecimals());
              profile.setConfirmationDepth(config.getConfirmationDepth());
              if (capability != null) {
                profile.setTransferWithAuthorization(Boolean.TRUE.equals(capability.getTransferWithAuthorization()));
                profile.setPermit(Boolean.TRUE.equals(capability.getPermit()));
                profile.setApprove(capability.getApprove() == null || Boolean.TRUE.equals(capability.getApprove()));
                profile.setSmartContractSettlement(Boolean.TRUE.equals(capability.getSmartContractSettlement()));
                profile.setSettlementContractAddress(capability.getSettlementContractAddress());
              }
              return profile;
            })
            .toList());
  }

  private String capabilityKey(String chain, String token) {
    return normalize(chain) + "|" + normalize(token);
  }
}
