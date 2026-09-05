package io.swzxsyh.manager.security;

/** 管理端功能权限编码。 */
public final class ManagerPermission {

  private ManagerPermission() {}

  public static final String DASHBOARD_VIEW = "dashboard:view";
  public static final String ORDER_VIEW = "order:view";
  public static final String CALLBACK_MANAGE = "callback:manage";
  public static final String CALLBACK_REPLAY = "callback:replay";
  public static final String APPROVAL_MANAGE = "approval:manage";
  public static final String APPROVAL_REVIEW = "approval:review";
  public static final String APPROVAL_CONFIG_SAVE = "approval:config-save";
  public static final String AUDIT_VIEW = "audit:view";
  public static final String PAYMENT_EXCEPTION_MANAGE = "payment-exception:manage";
  public static final String PAYMENT_EXCEPTION_HANDLE = "payment-exception:handle";
  public static final String RAW_CHAIN_LOG_MANAGE = "raw-chain-log:manage";
  public static final String RAW_CHAIN_LOG_MANUAL_PROCESS = "raw-chain-log:manual-process";
  public static final String RECONCILIATION_MANAGE = "reconciliation:manage";
  public static final String RECONCILIATION_RUN = "reconciliation:run";
  public static final String RECONCILIATION_MANUAL_CONFIRM = "reconciliation:manual-confirm";
  public static final String MERCHANT_MANAGE = "merchant:manage";
  public static final String MERCHANT_SAVE = "merchant:save";
  public static final String MERCHANT_CHANNEL_SAVE = "merchant:channel-save";
  public static final String MERCHANT_PRODUCT_MANAGE = "merchant-product:manage";
  public static final String MERCHANT_PRODUCT_SAVE = "merchant-product:save";
  public static final String MERCHANT_BALANCE_MANAGE = "merchant-balance:manage";
  public static final String MERCHANT_BALANCE_ADJUST = "merchant-balance:adjust";
  public static final String MERCHANT_WITHDRAW_OPERATE = "merchant-withdraw:operate";
  public static final String CONFIG_MANAGE = "config:manage";
  public static final String CONFIG_SAVE = "config:save";
  public static final String TOKEN_ONBOARD = "config:token-onboard";
  public static final String RISK_MANAGE = "risk:manage";
  public static final String RISK_RULE_SAVE = "risk:rule-save";
  public static final String RISK_RULE_DELETE = "risk:rule-delete";
  public static final String SCANNER_MANAGE = "scanner:manage";
  public static final String SCANNER_CHECKPOINT_SAVE = "scanner:checkpoint-save";
  public static final String SUBSCRIPTION_MANAGE = "subscription:manage";
  public static final String SUBSCRIPTION_ORDER_OPERATE = "subscription:order-operate";
  public static final String SUBSCRIPTION_BILLING_OPERATE = "subscription:billing-operate";
  public static final String ADDRESS_POOL_MANAGE = "address-pool:manage";
  public static final String ADDRESS_POOL_RELEASE = "address-pool:release";
  public static final String ADDRESS_POOL_BLOCK = "address-pool:block";
  public static final String ADDRESS_POOL_RETIRE = "address-pool:retire";
  public static final String ADDRESS_POOL_POLICY_SAVE = "address-pool:policy-save";
  public static final String SETTLEMENT_MANAGE = "settlement:manage";
  public static final String SETTLEMENT_MARK = "settlement:mark";
  public static final String ROLE_SAVE = "security:role-save";
  public static final String USER_SAVE = "security:user-save";
}
