package io.swzxsyh.payment.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * 结构化配置总加载器。
 *
 * <p>本类只负责启动和手动 reload 编排，具体配置表到运行时配置的映射由各个
 * {@link PaymentConfigContributor} 实现承接，避免所有配置逻辑堆在一个类里。
 */
@Slf4j
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(
    prefix = "payment.normalized-config",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class PaymentNormalizedConfigLoader implements ApplicationRunner {

  private final CryptoPaymentProperties properties;
  private final List<PaymentConfigContributor> contributors;

  public PaymentNormalizedConfigLoader(
      CryptoPaymentProperties properties, List<PaymentConfigContributor> contributors) {
    this.properties = properties;
    this.contributors = contributors;
  }

  @Override
  public void run(ApplicationArguments args) {
    reloadFromDatabase();
  }

  /** 重新从数据库读取结构化配置，并刷新到运行时配置对象。 */
  public synchronized void reloadFromDatabase() {
    contributors.forEach(PaymentConfigContributor::apply);
    log.info("Loaded normalized payment config. chains={}, tokens={}, contributors={}",
        properties.getChainProfiles().size(),
        properties.getTokenProfiles().size(),
        contributors.size());
  }
}
