package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.AddressPoolSetupRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.AddressPoolSetupResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.ContractSplitSetupRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.ContractSplitSetupResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.MerchantOnboardingRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.MerchantOnboardingResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.PlatformSecuritySetupRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.PlatformSecuritySetupResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.SubscriptionSetupRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.SubscriptionSetupResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.KytSetupRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.KytSetupResponse;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.GatewayAccessSetupRequest;
import io.swzxsyh.manager.api.dto.ManagerSetupGuideDtos.GatewayAccessSetupResponse;
import io.swzxsyh.manager.application.ManagerSetupGuideApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** P0 初始化向导接口，面向 manager 端聚合配置写入。 */
@RestController
@RequestMapping("/manager/setup-guides")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_MANAGE)")
public class ManagerSetupGuideController {

  private final ManagerSetupGuideApplicationService setupGuideService;

  public ManagerSetupGuideController(ManagerSetupGuideApplicationService setupGuideService) {
    this.setupGuideService = setupGuideService;
  }

  /** 商户接入向导：商户、产品和商户可用链币费率在同一事务内保存。 */
  @PostMapping("/merchant-onboarding")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_SAVE)")
  public ManagerApiResponse<MerchantOnboardingResponse> onboardMerchant(
      @RequestBody MerchantOnboardingRequest request) {
    return ManagerApiResponse.ok(setupGuideService.onboardMerchant(request));
  }

  /** 地址池初始化向导：派生地址策略和地址池最小库存策略在同一事务内保存。 */
  @PostMapping("/address-pool")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ADDRESS_POOL_POLICY_SAVE)")
  public ManagerApiResponse<AddressPoolSetupResponse> setupAddressPool(
      @RequestBody AddressPoolSetupRequest request) {
    return ManagerApiResponse.ok(setupGuideService.setupAddressPool(request));
  }

  /** 智能合约分账向导：同一作用域下的分账比例规则在同一事务内保存。 */
  @PostMapping("/contract-split")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<ContractSplitSetupResponse> setupContractSplit(
      @RequestBody ContractSplitSetupRequest request) {
    return ManagerApiResponse.ok(setupGuideService.setupContractSplit(request));
  }

  /** 平台安全初始化向导：平台基础、收银台 Token 和安全策略在同一事务内保存。 */
  @PostMapping("/platform-security")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PlatformSecuritySetupResponse> setupPlatformSecurity(
      @RequestBody PlatformSecuritySetupRequest request) {
    return ManagerApiResponse.ok(setupGuideService.setupPlatformSecurity(request));
  }

  /** 订阅支付向导：订阅执行模式、合约地址、调度和重试策略统一保存。 */
  @PostMapping("/subscription")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<SubscriptionSetupResponse> setupSubscription(
      @RequestBody SubscriptionSetupRequest request) {
    return ManagerApiResponse.ok(setupGuideService.setupSubscription(request));
  }

  /** KYT 风控向导：全局阈值和黑白名单规则统一保存。 */
  @PostMapping("/kyt")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_SAVE)")
  public ManagerApiResponse<KytSetupResponse> setupKyt(@RequestBody KytSetupRequest request) {
    return ManagerApiResponse.ok(setupGuideService.setupKyt(request));
  }

  /** x402 / API 门禁向导：网关开关、服务费、API Key 和 discovery 配置统一保存。 */
  @PostMapping("/gateway-access")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<GatewayAccessSetupResponse> setupGatewayAccess(
      @RequestBody GatewayAccessSetupRequest request) {
    return ManagerApiResponse.ok(setupGuideService.setupGatewayAccess(request));
  }
}
