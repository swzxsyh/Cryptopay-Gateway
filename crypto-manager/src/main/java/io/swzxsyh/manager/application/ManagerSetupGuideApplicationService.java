package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.CashierRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.ContractSplitRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.PlatformRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.SecurityRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.KytRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.DiscoveryRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.GatewayRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.SubscriptionRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantView;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelBatchSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelView;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductView;
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
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.AddressRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.ChainRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.TokenRuleRequest;
import io.swzxsyh.payment.mapper.PaymentContractSplitRuleMapper;
import io.swzxsyh.payment.persistence.entity.PaymentCashierConfig;
import io.swzxsyh.payment.persistence.entity.PaymentContractSplitRule;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolPolicy;
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
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 管理端初始化向导编排服务，只负责串联已有领域能力，不承载支付执行逻辑。 */
@Service
public class ManagerSetupGuideApplicationService {

  private final ManagerMerchantApplicationService merchantService;
  private final ManagerMerchantProductApplicationService productService;
  private final ManagerRuntimeConfigApplicationService runtimeConfigService;
  private final ManagerAddressPoolApplicationService addressPoolService;
  private final ManagerRiskApplicationService riskService;
  private final PaymentContractSplitRuleMapper splitRuleMapper;

  public ManagerSetupGuideApplicationService(
      ManagerMerchantApplicationService merchantService,
      ManagerMerchantProductApplicationService productService,
      ManagerRuntimeConfigApplicationService runtimeConfigService,
      ManagerAddressPoolApplicationService addressPoolService,
      ManagerRiskApplicationService riskService,
      PaymentContractSplitRuleMapper splitRuleMapper) {
    this.merchantService = merchantService;
    this.productService = productService;
    this.runtimeConfigService = runtimeConfigService;
    this.addressPoolService = addressPoolService;
    this.riskService = riskService;
    this.splitRuleMapper = splitRuleMapper;
  }

  /** 商户接入向导：任一环节失败时回滚商户、产品和通道配置。 */
  @Transactional(rollbackFor = Exception.class)
  public MerchantOnboardingResponse onboardMerchant(MerchantOnboardingRequest request) {
    if (request == null || request.merchant() == null) {
      throw new IllegalArgumentException("merchant is required");
    }
    MerchantView merchant = merchantService.save(request.merchant());
    ProductView product = saveProductIfPresent(merchant.merchantId(), request.product());
    List<PaymentChannelView> channels = saveChannelsIfPresent(merchant.merchantId(), request.channel());
    return new MerchantOnboardingResponse(merchant, product, channels);
  }

  /** 地址池初始化向导：派生策略和池策略需要同时保存，避免只配一半导致订单取址失败。 */
  @Transactional(rollbackFor = Exception.class)
  public AddressPoolSetupResponse setupAddressPool(AddressPoolSetupRequest request) {
    if (request == null || request.derivedAddressConfig() == null || request.policy() == null) {
      throw new IllegalArgumentException("derivedAddressConfig and policy are required");
    }
    PaymentDerivedAddressConfig derivedAddressConfig =
        runtimeConfigService.saveDerivedAddress(request.derivedAddressConfig());
    DerivedAddressPoolPolicy policy = addressPoolService.saveAddressPolicy(request.policy());
    return new AddressPoolSetupResponse(derivedAddressConfig, policy);
  }

  /** 智能合约分账向导：校验分账比例合计为 10000 基点后，批量保存规则。 */
  @Transactional(rollbackFor = Exception.class)
  public ContractSplitSetupResponse setupContractSplit(ContractSplitSetupRequest request) {
    if (request == null || !StringUtils.hasText(request.configScope())
        || request.rules() == null || request.rules().isEmpty()) {
      throw new IllegalArgumentException("configScope and rules are required");
    }
    String configScope = request.configScope().trim();
    int totalBasisPoints = request.rules().stream()
        .mapToInt(rule -> rule.basisPoints() == null ? 0 : rule.basisPoints())
        .sum();
    if (totalBasisPoints != 10000) {
      throw new IllegalArgumentException("split rule total basisPoints must be 10000");
    }
    splitRuleMapper.delete(Wrappers.<PaymentContractSplitRule>lambdaQuery()
        .eq(PaymentContractSplitRule::getConfigScope, configScope));
    List<PaymentContractSplitRule> saved = new ArrayList<>();
    int sortNo = 0;
    for (ContractSplitRuleRequest rule : request.rules()) {
      if (rule == null || !StringUtils.hasText(rule.roleCode())
          || !StringUtils.hasText(rule.receiverAddress())) {
        throw new IllegalArgumentException("roleCode and receiverAddress are required");
      }
      if (rule.basisPoints() == null || rule.basisPoints() <= 0) {
        throw new IllegalArgumentException("basisPoints must be greater than 0");
      }
      Integer effectiveSortNo = rule.sortNo() == null ? sortNo++ : rule.sortNo();
      saved.add(runtimeConfigService.saveSplitRule(new ContractSplitRuleRequest(
          rule.id(),
          configScope,
          rule.roleCode(),
          rule.receiverAddress(),
          rule.basisPoints(),
          effectiveSortNo,
          rule.enabled() == null || Boolean.TRUE.equals(rule.enabled()))));
    }
    return new ContractSplitSetupResponse(configScope, totalBasisPoints, saved);
  }

  /** 平台安全初始化向导：平台基础、收银台 Token 和开放重定向策略统一保存。 */
  @Transactional(rollbackFor = Exception.class)
  public PlatformSecuritySetupResponse setupPlatformSecurity(PlatformSecuritySetupRequest request) {
    if (request == null || request.platform() == null || request.cashier() == null || request.security() == null) {
      throw new IllegalArgumentException("platform, cashier and security are required");
    }
    PaymentPlatformConfig platform = runtimeConfigService.savePlatform(withRequiredPlatformDefaults(request.platform()));
    PaymentCashierConfig cashier = runtimeConfigService.saveCashier(withRequiredCashierDefaults(request.cashier()));
    PaymentSecurityConfig security = runtimeConfigService.saveSecurity(withRequiredSecurityDefaults(request.security()));
    return new PlatformSecuritySetupResponse(platform, cashier, security);
  }

  /** 订阅支付向导：保存 ERC-1337/EIP-945 类扣款和 Superfluid 流式支付的执行配置。 */
  @Transactional(rollbackFor = Exception.class)
  public SubscriptionSetupResponse setupSubscription(SubscriptionSetupRequest request) {
    if (request == null || request.subscription() == null) {
      throw new IllegalArgumentException("subscription is required");
    }
    PaymentSubscriptionConfig subscription =
        runtimeConfigService.saveSubscription(withRequiredSubscriptionDefaults(request.subscription()));
    return new SubscriptionSetupResponse(subscription);
  }

  /** KYT 风控向导：保存全局阈值，并批量写入链、Token、地址规则。 */
  @Transactional(rollbackFor = Exception.class)
  public KytSetupResponse setupKyt(KytSetupRequest request) {
    if (request == null || request.kyt() == null) {
      throw new IllegalArgumentException("kyt is required");
    }
    PaymentKytConfig kyt = runtimeConfigService.saveKyt(withRequiredKytDefaults(request.kyt()));
    List<PaymentKytChainRule> chainRules = saveChainRules(request.chainRules());
    List<PaymentKytTokenRule> tokenRules = saveTokenRules(request.tokenRules());
    List<PaymentKytAddressRule> addressRules = saveAddressRules(request.addressRules());
    return new KytSetupResponse(kyt, chainRules, tokenRules, addressRules);
  }

  /** x402 / API 门禁向导：保存 x402 网关收费、API Key 和 discovery 能力自描述配置。 */
  @Transactional(rollbackFor = Exception.class)
  public GatewayAccessSetupResponse setupGatewayAccess(GatewayAccessSetupRequest request) {
    if (request == null || request.gateway() == null || request.discovery() == null) {
      throw new IllegalArgumentException("gateway and discovery are required");
    }
    PaymentGatewayConfig gateway = runtimeConfigService.saveGateway(withRequiredGatewayDefaults(request.gateway()));
    PaymentDiscoveryConfig discovery =
        runtimeConfigService.saveDiscovery(withRequiredDiscoveryDefaults(request.discovery()));
    return new GatewayAccessSetupResponse(gateway, discovery);
  }

  private ProductView saveProductIfPresent(String merchantId, ProductSaveRequest request) {
    if (request == null || !StringUtils.hasText(request.productName())) {
      return null;
    }
    return productService.save(new ProductSaveRequest(
        request.id(),
        merchantId,
        request.productName(),
        request.productUrl(),
        request.testUsername(),
        request.testPassword(),
        request.remark()));
  }

  private List<PaymentChannelView> saveChannelsIfPresent(String merchantId, PaymentChannelBatchSaveRequest request) {
    if (request == null || request.selections() == null || request.selections().isEmpty()) {
      return List.of();
    }
    return merchantService.savePaymentChannels(new PaymentChannelBatchSaveRequest(
        merchantId,
        request.selections(),
        request.enabled(),
        request.transactionFeeRate(),
        request.minimumFee(),
        request.fixedFee(),
        request.gatewayFee(),
        request.taxRate(),
        request.minOrderAmount(),
        request.maxOrderAmount(),
        request.remark()));
  }

  private PlatformRequest withRequiredPlatformDefaults(PlatformRequest request) {
    return new PlatformRequest(
        request.id(),
        defaultScope(request.configScope()),
        request.orderExpireMinutes() == null ? 15 : request.orderExpireMinutes(),
        request.cashierBaseUrl(),
        request.treasuryAddress(),
        request.idempotencyWaitMillis() == null ? 4000L : request.idempotencyWaitMillis(),
        request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
  }

  private CashierRequest withRequiredCashierDefaults(CashierRequest request) {
    return new CashierRequest(
        request.id(),
        defaultScope(request.configScope()),
        request.tokenEncryptionEnabled() == null || Boolean.TRUE.equals(request.tokenEncryptionEnabled()),
        request.tokenKeyAlias(),
        request.tokenTtlMinutes() == null ? 30 : request.tokenTtlMinutes(),
        StringUtils.hasText(request.tokenPrefix()) ? request.tokenPrefix() : "cashier",
        request.shortTokenEnabled() == null || Boolean.TRUE.equals(request.shortTokenEnabled()),
        StringUtils.hasText(request.shortTokenPrefix()) ? request.shortTokenPrefix() : "C",
        request.shortTokenLength() == null ? 12 : request.shortTokenLength());
  }

  private SecurityRequest withRequiredSecurityDefaults(SecurityRequest request) {
    return new SecurityRequest(
        request.id(),
        defaultScope(request.configScope()),
        request.validateRedirectUrl() == null || Boolean.TRUE.equals(request.validateRedirectUrl()),
        Boolean.TRUE.equals(request.allowLocalRedirect()));
  }

  private SubscriptionRequest withRequiredSubscriptionDefaults(SubscriptionRequest request) {
    return new SubscriptionRequest(
        request.id(),
        defaultScope(request.configScope()),
        request.subscriptionEnabled() == null || Boolean.TRUE.equals(request.subscriptionEnabled()),
        StringUtils.hasText(request.defaultMode()) ? request.defaultMode().trim().toUpperCase() : "ERC1337",
        request.defaultCycleSeconds() == null ? 2592000 : request.defaultCycleSeconds(),
        request.superfluidHostAddress(),
        request.superfluidCfaAddress(),
        request.erc1337ExecutorAddress(),
        request.schedulerEnabled() == null || Boolean.TRUE.equals(request.schedulerEnabled()),
        request.schedulerBatchSize() == null ? 100 : request.schedulerBatchSize(),
        request.maxRetryCount() == null ? 3 : request.maxRetryCount(),
        request.retryBackoffSeconds() == null ? 300 : request.retryBackoffSeconds(),
        request.executionGasLimit(),
        StringUtils.hasText(request.executorPrivateKeySourceType())
            ? request.executorPrivateKeySourceType().trim().toUpperCase()
            : "ENV",
        request.executorPrivateKeyEnv(),
        request.executorPrivateKeyKmsKeyId());
  }

  private KytRequest withRequiredKytDefaults(KytRequest request) {
    return new KytRequest(
        request.id(),
        defaultScope(request.configScope()),
        Boolean.TRUE.equals(request.kytEnabled()),
        Boolean.TRUE.equals(request.strictMode()),
        request.reviewThreshold() == null ? 60 : request.reviewThreshold(),
        request.rejectThreshold() == null ? 80 : request.rejectThreshold());
  }

  private GatewayRequest withRequiredGatewayDefaults(GatewayRequest request) {
    return new GatewayRequest(
        request.id(),
        defaultScope(request.configScope()),
        request.gatewayEnabled() == null || Boolean.TRUE.equals(request.gatewayEnabled()),
        request.serviceFeeToken(),
        request.serviceFeeAmount(),
        request.apiKey(),
        Boolean.TRUE.equals(request.publicResourcesEnabled()),
        StringUtils.hasText(request.facilitatorName()) ? request.facilitatorName().trim() : "swzxsyh-facilitator");
  }

  private DiscoveryRequest withRequiredDiscoveryDefaults(DiscoveryRequest request) {
    return new DiscoveryRequest(
        request.id(),
        defaultScope(request.configScope()),
        request.discoveryEnabled() == null || Boolean.TRUE.equals(request.discoveryEnabled()),
        request.publicBaseUrl());
  }

  private List<PaymentKytChainRule> saveChainRules(List<ChainRuleRequest> requests) {
    if (requests == null) {
      return List.of();
    }
    return requests.stream()
        .filter(rule -> rule != null && StringUtils.hasText(rule.chainCode()))
        .map(rule -> riskService.saveChainRule(new ChainRuleRequest(
            rule.id(),
            rule.chainCode().trim().toUpperCase(),
            normalizeRuleType(rule.ruleType()),
            rule.enabled() == null || Boolean.TRUE.equals(rule.enabled()))))
        .toList();
  }

  private List<PaymentKytTokenRule> saveTokenRules(List<TokenRuleRequest> requests) {
    if (requests == null) {
      return List.of();
    }
    return requests.stream()
        .filter(rule -> rule != null && StringUtils.hasText(rule.tokenSymbol()))
        .map(rule -> riskService.saveTokenRule(new TokenRuleRequest(
            rule.id(),
            StringUtils.hasText(rule.chainCode()) ? rule.chainCode().trim().toUpperCase() : null,
            rule.tokenSymbol().trim().toUpperCase(),
            normalizeRuleType(rule.ruleType()),
            rule.enabled() == null || Boolean.TRUE.equals(rule.enabled()))))
        .toList();
  }

  private List<PaymentKytAddressRule> saveAddressRules(List<AddressRuleRequest> requests) {
    if (requests == null) {
      return List.of();
    }
    return requests.stream()
        .filter(rule -> rule != null && StringUtils.hasText(rule.address()))
        .map(rule -> riskService.saveAddressRule(new AddressRuleRequest(
            rule.id(),
            rule.address().trim(),
            normalizeRuleType(rule.ruleType()),
            rule.enabled() == null || Boolean.TRUE.equals(rule.enabled()))))
        .toList();
  }

  private String normalizeRuleType(String ruleType) {
    return StringUtils.hasText(ruleType) ? ruleType.trim().toUpperCase() : "BLACKLIST";
  }

  private String defaultScope(String configScope) {
    return StringUtils.hasText(configScope) ? configScope.trim() : "GLOBAL";
  }
}
