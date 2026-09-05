package io.swzxsyh.payment.application;

import java.lang.management.ManagementFactory;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 应用实例标识，用于 WSS leader 租约、消息消费和运行告警。 */
@Component
public class PaymentInstanceIdentity {

  private final String instanceId =
      ManagementFactory.getRuntimeMXBean().getName() + "-" + UUID.randomUUID().toString().substring(0, 8);

  public String instanceId() {
    return instanceId;
  }
}
