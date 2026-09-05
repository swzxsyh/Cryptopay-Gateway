package io.swzxsyh.payment.application;

import io.swzxsyh.payment.alert.PaymentAlertService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** payment 实例运行状态通知器；当前只通过日志和告警发送，不写 Redis 健康状态。 */
@Slf4j
@Component
public class PaymentInstanceHealthNotifier {

  private final CryptoPaymentProperties properties;
  private final PaymentAlertService alertService;
  private final PaymentInstanceIdentity identity;

  public PaymentInstanceHealthNotifier(
      CryptoPaymentProperties properties,
      PaymentAlertService alertService,
      PaymentInstanceIdentity identity) {
    this.properties = properties;
    this.alertService = alertService;
    this.identity = identity;
  }

  /** 应用可服务后发送实例启动通知。 */
  @EventListener(ApplicationReadyEvent.class)
  public void notifyStarted() {
    int chainCount = properties.getChainProfiles().size();
    log.info("payment 实例已启动。instanceId={}, chainCount={}", identity.instanceId(), chainCount);
    alertService.alertPaymentInstanceStarted(identity.instanceId(), chainCount);
  }

  /** 应用关闭前发送实例停止通知。 */
  @PreDestroy
  public void notifyStopped() {
    log.info("payment 实例准备停止。instanceId={}", identity.instanceId());
    alertService.alertPaymentInstanceStopped(identity.instanceId());
  }
}
