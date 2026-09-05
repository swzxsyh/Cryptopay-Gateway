package io.swzxsyh.payment.messaging;

import java.util.function.Consumer;

/** 链上入账事件订阅端口；真实 MQ 接入时替换实现，不影响 payment 应用编排代码。 */
public interface ChainPaymentEventSubscriber {

  /** 注册事件处理器，返回订阅句柄用于应用关闭时释放资源。 */
  Subscription subscribe(Consumer<ChainPaymentEvent> handler);

  /** MQ 订阅句柄。 */
  interface Subscription {

    /** 关闭订阅并释放底层监听资源。 */
    void close();
  }
}
