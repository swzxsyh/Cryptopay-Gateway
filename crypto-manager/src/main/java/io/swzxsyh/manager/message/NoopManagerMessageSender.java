package io.swzxsyh.manager.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 默认空实现：只记录日志，不真正外发，避免测试环境误触达。 */
@Component
public class NoopManagerMessageSender implements ManagerMessageSender {

  private static final Logger log = LoggerFactory.getLogger(NoopManagerMessageSender.class);

  @Override
  public ManagerMessageSendResult send(ManagerMessageRequest request) {
    log.info("Manager message noop sent. templateCode={}, channel={}, receiver={}",
        request.templateCode(), request.channel(), request.receiver());
    return new ManagerMessageSendResult(true, "NOOP", null, "noop");
  }
}
