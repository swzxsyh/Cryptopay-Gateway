package io.swzxsyh.payment.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.PaymentKytAddressRuleMapper;
import io.swzxsyh.payment.mapper.PaymentKytChainRuleMapper;
import io.swzxsyh.payment.mapper.PaymentKytConfigMapper;
import io.swzxsyh.payment.mapper.PaymentKytTokenRuleMapper;
import io.swzxsyh.payment.persistence.entity.PaymentKytAddressRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytChainRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytTokenRule;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** KYT 开关、阈值和本地风控白黑名单配置加载器。 */
@Component
@Order(110)
public class KytConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentKytConfigMapper kytMapper;
  private final PaymentKytAddressRuleMapper addressRuleMapper;
  private final PaymentKytChainRuleMapper chainRuleMapper;
  private final PaymentKytTokenRuleMapper tokenRuleMapper;

  public KytConfigLoader(
      CryptoPaymentProperties properties,
      PaymentKytConfigMapper kytMapper,
      PaymentKytAddressRuleMapper addressRuleMapper,
      PaymentKytChainRuleMapper chainRuleMapper,
      PaymentKytTokenRuleMapper tokenRuleMapper) {
    this.properties = properties;
    this.kytMapper = kytMapper;
    this.addressRuleMapper = addressRuleMapper;
    this.chainRuleMapper = chainRuleMapper;
    this.tokenRuleMapper = tokenRuleMapper;
  }

  @Override
  public void apply() {
    first(kytMapper.selectList(null)).ifPresent(config -> {
      CryptoPaymentProperties.Kyt kyt = properties.getKyt();
      setIfPresent(config.getKytEnabled(), kyt::setEnabled);
      setIfPresent(config.getStrictMode(), kyt::setStrictMode);
      setIfPresent(config.getReviewThreshold(), kyt::setReviewThreshold);
      setIfPresent(config.getRejectThreshold(), kyt::setRejectThreshold);
    });
    applyAddressRules();
    applyChainAndTokenRules();
  }

  private void applyAddressRules() {
    List<PaymentKytAddressRule> rules =
        addressRuleMapper.selectList(Wrappers.<PaymentKytAddressRule>lambdaQuery()
            .eq(PaymentKytAddressRule::getEnabled, Boolean.TRUE));
    properties.getKyt().setAllowAddresses(rules.stream()
        .filter(rule -> "ALLOW".equalsIgnoreCase(rule.getRuleType()))
        .map(PaymentKytAddressRule::getAddress)
        .filter(StringUtils::hasText)
        .toList());
    properties.getKyt().setDenyAddresses(rules.stream()
        .filter(rule -> "DENY".equalsIgnoreCase(rule.getRuleType()))
        .map(PaymentKytAddressRule::getAddress)
        .filter(StringUtils::hasText)
        .toList());
  }

  private void applyChainAndTokenRules() {
    properties.getKyt().setHighRiskChains(
        chainRuleMapper.selectList(Wrappers.<PaymentKytChainRule>lambdaQuery()
                .eq(PaymentKytChainRule::getEnabled, Boolean.TRUE)
                .eq(PaymentKytChainRule::getRuleType, "HIGH_RISK"))
            .stream()
            .map(PaymentKytChainRule::getChainCode)
            .filter(StringUtils::hasText)
            .toList());
    properties.getKyt().setHighRiskTokens(
        tokenRuleMapper.selectList(Wrappers.<PaymentKytTokenRule>lambdaQuery()
                .eq(PaymentKytTokenRule::getEnabled, Boolean.TRUE)
                .eq(PaymentKytTokenRule::getRuleType, "HIGH_RISK"))
            .stream()
            .map(PaymentKytTokenRule::getTokenSymbol)
            .filter(StringUtils::hasText)
            .toList());
  }
}
