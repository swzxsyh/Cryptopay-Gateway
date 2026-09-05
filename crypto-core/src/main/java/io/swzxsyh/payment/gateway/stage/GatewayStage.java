package io.swzxsyh.payment.gateway.stage;

import io.swzxsyh.payment.gateway.model.GatewayContext;

/** 网关编排阶段抽象。 */
public interface GatewayStage {

  void apply(GatewayContext context);
}
