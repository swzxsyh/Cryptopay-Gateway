package io.swzxsyh.payment.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.mapper.PaymentRedirectHostMapper;
import io.swzxsyh.payment.mapper.PaymentSecurityConfigMapper;
import io.swzxsyh.payment.persistence.entity.PaymentRedirectHost;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 安全配置与 returnUrl 白名单加载器。 */
@Component
@Order(40)
public class SecurityConfigLoader extends PaymentConfigLoaderSupport implements PaymentConfigContributor {

  private final CryptoPaymentProperties properties;
  private final PaymentSecurityConfigMapper securityMapper;
  private final PaymentRedirectHostMapper redirectHostMapper;

  public SecurityConfigLoader(
      CryptoPaymentProperties properties,
      PaymentSecurityConfigMapper securityMapper,
      PaymentRedirectHostMapper redirectHostMapper) {
    this.properties = properties;
    this.securityMapper = securityMapper;
    this.redirectHostMapper = redirectHostMapper;
  }

  @Override
  public void apply() {
    first(securityMapper.selectList(null))
        .ifPresent(
            config -> {
              CryptoPaymentProperties.Security security = properties.getSecurity();
              setIfPresent(config.getValidateRedirectUrl(), security::setValidateRedirectUrl);
              setIfPresent(config.getAllowLocalRedirect(), security::setAllowLocalRedirect);
            });
    properties
        .getSecurity()
        .setAllowedRedirectHosts(
            redirectHostMapper
                .selectList(
                    Wrappers.<PaymentRedirectHost>lambdaQuery()
                        .eq(PaymentRedirectHost::getEnabled, Boolean.TRUE))
                .stream()
                .map(PaymentRedirectHost::getHost)
                .filter(StringUtils::hasText)
                .toList());
  }
}
