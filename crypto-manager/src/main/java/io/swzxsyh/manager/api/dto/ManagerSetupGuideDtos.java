package io.swzxsyh.manager.api.dto;

import io.swzxsyh.manager.api.dto.ManagerAddressPoolDtos.PolicyRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.CashierRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.ContractSplitRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.DerivedAddressRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.PlatformRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.SecurityRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.KytRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.DiscoveryRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.GatewayRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.SubscriptionRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantView;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelBatchSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelView;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductView;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.AddressRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.ChainRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.TokenRuleRequest;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolPolicy;
import io.swzxsyh.payment.persistence.entity.PaymentCashierConfig;
import io.swzxsyh.payment.persistence.entity.PaymentContractSplitRule;
import io.swzxsyh.payment.persistence.entity.PaymentDerivedAddressConfig;
import io.swzxsyh.payment.persistence.entity.PaymentKytAddressRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytChainRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytConfig;
import io.swzxsyh.payment.persistence.entity.PaymentKytTokenRule;
import io.swzxsyh.payment.persistence.entity.PaymentDiscoveryConfig;
import io.swzxsyh.payment.persistence.entity.PaymentGatewayConfig;
import io.swzxsyh.payment.persistence.entity.PaymentPlatformConfig;
import io.swzxsyh.payment.persistence.entity.PaymentSecurityConfig;
import io.swzxsyh.payment.persistence.entity.PaymentSubscriptionConfig;
import java.util.List;

/** 初始化向导 DTO；一个向导请求对应一个服务端事务。 */
public final class ManagerSetupGuideDtos {

  private ManagerSetupGuideDtos() {}

  /** 商户接入向导请求：商户主数据、可选产品资料、可选支付链币费率统一提交。 */
  public record MerchantOnboardingRequest(
      MerchantSaveRequest merchant,
      ProductSaveRequest product,
      PaymentChannelBatchSaveRequest channel) {}

  /** 商户接入向导响应。 */
  public record MerchantOnboardingResponse(
      MerchantView merchant,
      ProductView product,
      List<PaymentChannelView> channels) {}

  /** 地址池初始化向导请求：派生地址生成策略和地址池最小可用量策略统一提交。 */
  public record AddressPoolSetupRequest(
      DerivedAddressRequest derivedAddressConfig,
      PolicyRequest policy) {}

  /** 地址池初始化向导响应。 */
  public record AddressPoolSetupResponse(
      PaymentDerivedAddressConfig derivedAddressConfig,
      DerivedAddressPoolPolicy policy) {}

  /** 智能合约分账向导请求：同一作用域下的分账规则整体提交。 */
  public record ContractSplitSetupRequest(
      String configScope,
      List<ContractSplitRuleRequest> rules) {}

  /** 智能合约分账向导响应。 */
  public record ContractSplitSetupResponse(
      String configScope,
      Integer totalBasisPoints,
      List<PaymentContractSplitRule> rules) {}

  /** 平台安全初始化向导请求：平台、收银台令牌、安全策略统一提交。 */
  public record PlatformSecuritySetupRequest(
      PlatformRequest platform,
      CashierRequest cashier,
      SecurityRequest security) {}

  /** 平台安全初始化向导响应。 */
  public record PlatformSecuritySetupResponse(
      PaymentPlatformConfig platform,
      PaymentCashierConfig cashier,
      PaymentSecurityConfig security) {}

  /** 订阅支付向导请求：订阅模式、执行器和调度参数统一提交。 */
  public record SubscriptionSetupRequest(SubscriptionRequest subscription) {}

  /** 订阅支付向导响应。 */
  public record SubscriptionSetupResponse(PaymentSubscriptionConfig subscription) {}

  /** KYT 风控向导请求：全局阈值和黑白名单规则统一提交。 */
  public record KytSetupRequest(
      KytRequest kyt,
      List<ChainRuleRequest> chainRules,
      List<TokenRuleRequest> tokenRules,
      List<AddressRuleRequest> addressRules) {}

  /** KYT 风控向导响应。 */
  public record KytSetupResponse(
      PaymentKytConfig kyt,
      List<PaymentKytChainRule> chainRules,
      List<PaymentKytTokenRule> tokenRules,
      List<PaymentKytAddressRule> addressRules) {}

  /** x402 / API 门禁向导请求：网关服务费、API Key、能力发现统一提交。 */
  public record GatewayAccessSetupRequest(
      GatewayRequest gateway,
      DiscoveryRequest discovery) {}

  /** x402 / API 门禁向导响应。 */
  public record GatewayAccessSetupResponse(
      PaymentGatewayConfig gateway,
      PaymentDiscoveryConfig discovery) {}
}
