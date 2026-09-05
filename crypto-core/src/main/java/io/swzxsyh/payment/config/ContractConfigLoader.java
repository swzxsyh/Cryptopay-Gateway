package io.swzxsyh.payment.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.PaymentContractConfigMapper;
import io.swzxsyh.payment.mapper.PaymentContractSplitRuleMapper;
import io.swzxsyh.payment.persistence.entity.PaymentContractSplitRule;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 智能合约与分账规则配置加载器。 */
@Component
@Order(30)
public class ContractConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentContractConfigMapper contractMapper;
  private final PaymentContractSplitRuleMapper splitRuleMapper;

  public ContractConfigLoader(
      CryptoPaymentProperties properties,
      PaymentContractConfigMapper contractMapper,
      PaymentContractSplitRuleMapper splitRuleMapper) {
    this.properties = properties;
    this.contractMapper = contractMapper;
    this.splitRuleMapper = splitRuleMapper;
  }

  @Override
  public void apply() {
    first(contractMapper.selectList(null))
        .ifPresent(
            config -> {
              CryptoPaymentProperties.Contract contract = properties.getContract();
              setIfPresent(config.getContractEnabled(), contract::setEnabled);
              setIfText(config.getContractAddress(), contract::setAddress);
              setIfPresent(config.getCreate2Enabled(), contract::setCreate2Enabled);
              setIfPresent(config.getCreate2HostedWalletOnly(), contract::setCreate2HostedWalletOnly);
              setIfText(config.getCreate2FactoryAddress(), contract::setCreate2FactoryAddress);
              setIfText(config.getCreate2InitCodeHash(), contract::setCreate2InitCodeHash);
              setIfText(config.getCreate2SaltPrefix(), contract::setCreate2SaltPrefix);
            });

    properties
        .getContract()
        .setSettlementRules(
            splitRuleMapper
                .selectList(
                    Wrappers.<PaymentContractSplitRule>lambdaQuery()
                        .eq(PaymentContractSplitRule::getEnabled, Boolean.TRUE)
                        .orderByAsc(PaymentContractSplitRule::getSortNo))
                .stream()
                .map(
                    rule -> {
                      CryptoPaymentProperties.SettlementRule target =
                          new CryptoPaymentProperties.SettlementRule();
                      target.setRole(rule.getRoleCode());
                      target.setReceiver(rule.getReceiverAddress());
                      target.setBasisPoints(
                          rule.getBasisPoints() == null ? 0 : rule.getBasisPoints());
                      return target;
                    })
                .toList());
  }
}
