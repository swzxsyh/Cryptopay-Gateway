package io.swzxsyh.payment.subscription.contract;

import io.swzxsyh.payment.subscription.SubscriptionBillingMode;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import io.swzxsyh.payment.subscription.dto.CreateSubscriptionOrderRequest;
import io.swzxsyh.payment.subscription.model.SubscriptionSetupPlan;

/** 订阅合约适配器，隔离不同协议的 ABI、执行地址和链上交易细节。 */
public interface SubscriptionContractAdapter {

  /** 当前适配器支持的订阅类型。 */
  SubscriptionBillingMode mode();

  /** 构造给前端钱包调用的初始化订阅 calldata。 */
  SubscriptionSetupPlan prepareSetup(SubscriptionOrder order, CreateSubscriptionOrderRequest request);

  /** 平台执行器发起一期扣款。没有配置执行私钥时返回 skipped。 */
  SubscriptionExecutionResult executeBilling(SubscriptionOrder order, SubscriptionBillingRecord bill);

  /** 暂停订阅的链上执行。 */
  SubscriptionExecutionResult pause(SubscriptionOrder order);

  /** 恢复订阅的链上执行。 */
  SubscriptionExecutionResult resume(SubscriptionOrder order);

  /** 取消订阅的链上执行。 */
  SubscriptionExecutionResult cancel(SubscriptionOrder order);
}
