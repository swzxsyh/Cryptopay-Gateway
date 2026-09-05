package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.CallbackRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.CashierRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.ChainRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.ContractSplitRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.DerivedAddressRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.DiscoveryRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.GasRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.GatewayRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.KytRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.PlatformRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.ScannerRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.SecurityRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.SubscriptionRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.TokenRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.TokenOnboardingRequest;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerRuntimeConfigApplicationService;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackConfig;
import io.swzxsyh.payment.persistence.entity.PaymentCashierConfig;
import io.swzxsyh.payment.persistence.entity.PaymentChainConfig;
import io.swzxsyh.payment.persistence.entity.PaymentContractSplitRule;
import io.swzxsyh.payment.persistence.entity.PaymentDerivedAddressConfig;
import io.swzxsyh.payment.persistence.entity.PaymentDiscoveryConfig;
import io.swzxsyh.payment.persistence.entity.PaymentGasConfig;
import io.swzxsyh.payment.persistence.entity.PaymentGatewayConfig;
import io.swzxsyh.payment.persistence.entity.PaymentKytConfig;
import io.swzxsyh.payment.persistence.entity.PaymentPlatformConfig;
import io.swzxsyh.payment.persistence.entity.PaymentScannerConfig;
import io.swzxsyh.payment.persistence.entity.PaymentSecurityConfig;
import io.swzxsyh.payment.persistence.entity.PaymentSubscriptionConfig;
import io.swzxsyh.payment.persistence.entity.PaymentTokenConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/config")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_MANAGE)")
public class ManagerRuntimeConfigController {

  private final ManagerRuntimeConfigApplicationService configService;

  public ManagerRuntimeConfigController(ManagerRuntimeConfigApplicationService configService) {
    this.configService = configService;
  }

  @GetMapping("/chains")
  /** 分页查询支付链配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentChainConfig>> chains(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(configService.pageChains(page, size, enabled));
  }

  @PostMapping("/chains")
  /** 新增或更新支付链配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentChainConfig> saveChain(
      @RequestBody ChainRequest request) {
    return ManagerApiResponse.ok(configService.saveChain(request));
  }

  @GetMapping("/tokens")
  /** 分页查询 Token 配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentTokenConfig>> tokens(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String chainCode,
      @RequestParam(required = false) String tokenSymbol,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(
        configService.pageTokens(page, size, chainCode, tokenSymbol, enabled));
  }

  @PostMapping("/tokens")
  /** 新增或更新 Token 配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentTokenConfig> saveToken(
      @RequestBody TokenRequest request) {
    return ManagerApiResponse.ok(configService.saveToken(request));
  }

  @PostMapping("/tokens/onboard")
  /** 代币接入向导：链、代币、能力、扫描器、Gas 配置统一事务提交。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).TOKEN_ONBOARD)")
  public ManagerApiResponse<PaymentTokenConfig> onboardToken(
      @RequestBody TokenOnboardingRequest request) {
    return ManagerApiResponse.ok(configService.onboardToken(request));
  }

  @GetMapping("/callbacks")
  /** 分页查询回调配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentCallbackConfig>> callbacks(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageCallbacks(page, size));
  }

  @PostMapping("/callbacks")
  /** 新增或更新回调配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentCallbackConfig> saveCallback(
      @RequestBody CallbackRequest request) {
    return ManagerApiResponse.ok(configService.saveCallback(request));
  }

  @GetMapping("/kyt")
  /** 分页查询 KYT 全局配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentKytConfig>> kyt(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageKyt(page, size));
  }

  @PostMapping("/kyt")
  /** 新增或更新 KYT 全局配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentKytConfig> saveKyt(
      @RequestBody KytRequest request) {
    return ManagerApiResponse.ok(configService.saveKyt(request));
  }

  @GetMapping("/derived-addresses")
  /** 分页查询派生地址策略配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentDerivedAddressConfig>> derivedAddresses(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageDerivedAddresses(page, size));
  }

  @PostMapping("/derived-addresses")
  /** 新增或更新派生地址策略配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentDerivedAddressConfig> saveDerivedAddress(
      @RequestBody DerivedAddressRequest request) {
    return ManagerApiResponse.ok(configService.saveDerivedAddress(request));
  }

  @GetMapping("/scanners")
  /** 分页查询扫描器策略配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentScannerConfig>> scanners(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageScanners(page, size));
  }

  @PostMapping("/scanners")
  /** 新增或更新扫描器策略配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentScannerConfig> saveScanner(
      @RequestBody ScannerRequest request) {
    return ManagerApiResponse.ok(configService.saveScanner(request));
  }

  @GetMapping("/gas")
  /** 分页查询 Gas 策略配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentGasConfig>> gas(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageGas(page, size));
  }

  @PostMapping("/gas")
  /** 新增或更新 Gas 策略配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentGasConfig> saveGas(
      @RequestBody GasRequest request) {
    return ManagerApiResponse.ok(configService.saveGas(request));
  }

  @GetMapping("/gateways")
  /** 分页查询网关配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentGatewayConfig>> gateways(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageGateways(page, size));
  }

  @PostMapping("/gateways")
  /** 新增或更新网关配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentGatewayConfig> saveGateway(
      @RequestBody GatewayRequest request) {
    return ManagerApiResponse.ok(configService.saveGateway(request));
  }

  @GetMapping("/subscriptions")
  /** 分页查询订阅支付配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentSubscriptionConfig>> subscriptions(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageSubscriptions(page, size));
  }

  @PostMapping("/subscriptions")
  /** 新增或更新订阅支付配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentSubscriptionConfig> saveSubscription(
      @RequestBody SubscriptionRequest request) {
    return ManagerApiResponse.ok(configService.saveSubscription(request));
  }

  @GetMapping("/platforms")
  /** 分页查询平台基础配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentPlatformConfig>> platforms(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pagePlatforms(page, size));
  }

  @PostMapping("/platforms")
  /** 新增或更新平台基础配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentPlatformConfig> savePlatform(
      @RequestBody PlatformRequest request) {
    return ManagerApiResponse.ok(configService.savePlatform(request));
  }

  @GetMapping("/cashiers")
  /** 分页查询收银台 Token 配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentCashierConfig>> cashiers(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageCashiers(page, size));
  }

  @PostMapping("/cashiers")
  /** 新增或更新收银台 Token 配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentCashierConfig> saveCashier(
      @RequestBody CashierRequest request) {
    return ManagerApiResponse.ok(configService.saveCashier(request));
  }

  @GetMapping("/security")
  /** 分页查询安全策略配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentSecurityConfig>> security(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageSecurity(page, size));
  }

  @PostMapping("/security")
  /** 新增或更新安全策略配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentSecurityConfig> saveSecurity(
      @RequestBody SecurityRequest request) {
    return ManagerApiResponse.ok(configService.saveSecurity(request));
  }

  @GetMapping("/discovery")
  /** 分页查询能力自描述配置。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentDiscoveryConfig>> discovery(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size) {
    return ManagerApiResponse.ok(configService.pageDiscovery(page, size));
  }

  @PostMapping("/discovery")
  /** 新增或更新能力自描述配置。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentDiscoveryConfig> saveDiscovery(
      @RequestBody DiscoveryRequest request) {
    return ManagerApiResponse.ok(configService.saveDiscovery(request));
  }

  @GetMapping("/contract-split-rules")
  /** 分页查询合约分账规则。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentContractSplitRule>> splitRules(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String configScope,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(
        configService.pageSplitRules(page, size, configScope, enabled));
  }

  @PostMapping("/contract-split-rules")
  /** 新增或更新合约分账规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CONFIG_SAVE)")
  public ManagerApiResponse<PaymentContractSplitRule> saveSplitRule(
      @RequestBody ContractSplitRuleRequest request) {
    return ManagerApiResponse.ok(configService.saveSplitRule(request));
  }
}
