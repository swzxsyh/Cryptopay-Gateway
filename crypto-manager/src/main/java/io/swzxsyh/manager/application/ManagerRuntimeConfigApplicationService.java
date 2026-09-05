package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.TokenCapabilityRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.TokenOnboardingRequest;
import io.swzxsyh.manager.api.dto.ManagerConfigDtos.TokenRequest;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.payment.mapper.PaymentCallbackConfigMapper;
import io.swzxsyh.payment.mapper.PaymentCashierConfigMapper;
import io.swzxsyh.payment.mapper.PaymentChainConfigMapper;
import io.swzxsyh.payment.mapper.PaymentContractSplitRuleMapper;
import io.swzxsyh.payment.mapper.PaymentDerivedAddressConfigMapper;
import io.swzxsyh.payment.mapper.PaymentDiscoveryConfigMapper;
import io.swzxsyh.payment.mapper.PaymentGasConfigMapper;
import io.swzxsyh.payment.mapper.PaymentGatewayConfigMapper;
import io.swzxsyh.payment.mapper.PaymentKytConfigMapper;
import io.swzxsyh.payment.mapper.PaymentPlatformConfigMapper;
import io.swzxsyh.payment.mapper.PaymentScannerConfigMapper;
import io.swzxsyh.payment.mapper.PaymentSecurityConfigMapper;
import io.swzxsyh.payment.mapper.PaymentSubscriptionConfigMapper;
import io.swzxsyh.payment.mapper.PaymentTokenCapabilityMapper;
import io.swzxsyh.payment.mapper.PaymentTokenConfigMapper;
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
import io.swzxsyh.payment.persistence.entity.PaymentTokenCapability;
import io.swzxsyh.payment.persistence.entity.PaymentTokenConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 管理端运行配置维护服务，集中处理链、Token、扫描、Gas、KYT、网关和安全配置。 */
@Service
public class ManagerRuntimeConfigApplicationService extends ManagerApplicationSupport {

  private final PaymentChainConfigMapper chainConfigMapper;
  private final PaymentTokenConfigMapper tokenConfigMapper;
  private final PaymentTokenCapabilityMapper tokenCapabilityMapper;
  private final PaymentCallbackConfigMapper callbackConfigMapper;
  private final PaymentCashierConfigMapper cashierConfigMapper;
  private final PaymentKytConfigMapper kytConfigMapper;
  private final PaymentScannerConfigMapper scannerConfigMapper;
  private final PaymentGasConfigMapper gasConfigMapper;
  private final PaymentGatewayConfigMapper gatewayConfigMapper;
  private final PaymentPlatformConfigMapper platformConfigMapper;
  private final PaymentContractSplitRuleMapper splitRuleMapper;
  private final PaymentDerivedAddressConfigMapper derivedAddressConfigMapper;
  private final PaymentSubscriptionConfigMapper subscriptionConfigMapper;
  private final PaymentSecurityConfigMapper securityConfigMapper;
  private final PaymentDiscoveryConfigMapper discoveryConfigMapper;

  public ManagerRuntimeConfigApplicationService(
      PaymentChainConfigMapper chainConfigMapper,
      PaymentTokenConfigMapper tokenConfigMapper,
      PaymentTokenCapabilityMapper tokenCapabilityMapper,
      PaymentCallbackConfigMapper callbackConfigMapper,
      PaymentCashierConfigMapper cashierConfigMapper,
      PaymentKytConfigMapper kytConfigMapper,
      PaymentScannerConfigMapper scannerConfigMapper,
      PaymentGasConfigMapper gasConfigMapper,
      PaymentGatewayConfigMapper gatewayConfigMapper,
      PaymentPlatformConfigMapper platformConfigMapper,
      PaymentContractSplitRuleMapper splitRuleMapper,
      PaymentDerivedAddressConfigMapper derivedAddressConfigMapper,
      PaymentSubscriptionConfigMapper subscriptionConfigMapper,
      PaymentSecurityConfigMapper securityConfigMapper,
      PaymentDiscoveryConfigMapper discoveryConfigMapper) {
    this.chainConfigMapper = chainConfigMapper;
    this.tokenConfigMapper = tokenConfigMapper;
    this.tokenCapabilityMapper = tokenCapabilityMapper;
    this.callbackConfigMapper = callbackConfigMapper;
    this.cashierConfigMapper = cashierConfigMapper;
    this.kytConfigMapper = kytConfigMapper;
    this.scannerConfigMapper = scannerConfigMapper;
    this.gasConfigMapper = gasConfigMapper;
    this.gatewayConfigMapper = gatewayConfigMapper;
    this.platformConfigMapper = platformConfigMapper;
    this.splitRuleMapper = splitRuleMapper;
    this.derivedAddressConfigMapper = derivedAddressConfigMapper;
    this.subscriptionConfigMapper = subscriptionConfigMapper;
    this.securityConfigMapper = securityConfigMapper;
    this.discoveryConfigMapper = discoveryConfigMapper;
  }

  /** 分页查询支付链配置。 */
  public ManagerPageResponse<PaymentChainConfig> pageChains(long page, long size, Boolean enabled) {
    return page(chainConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentChainConfig>lambdaQuery()
            .eq(enabled != null, PaymentChainConfig::getEnabled, enabled)
            .orderByAsc(PaymentChainConfig::getSortNo)
            .orderByAsc(PaymentChainConfig::getId)));
  }

  /** 新增或更新支付链配置。 */
  @Transactional
  public PaymentChainConfig saveChain(ChainRequest request) {
    if (request == null || !StringUtils.hasText(request.chainCode())) {
      throw new IllegalArgumentException("chainCode is required");
    }
    PaymentChainConfig entity = new PaymentChainConfig();
    entity.setId(request.id());
    entity.setChainCode(normalizeFilterCode(request.chainCode()));
    entity.setRpcUrl(trimTextToNull(request.rpcUrl()));
    entity.setWsUrl(trimTextToNull(request.wsUrl()));
    entity.setConfirmationDepth(request.confirmationDepth());
    entity.setEnabled(request.enabled());
    entity.setSortNo(request.sortNo());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> chainConfigMapper.insert(entity), () -> chainConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询 Token 配置。 */
  public ManagerPageResponse<PaymentTokenConfig> pageTokens(
      long page, long size, String chainCode, String tokenSymbol, Boolean enabled) {
    return page(tokenConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentTokenConfig>lambdaQuery()
            .eq(StringUtils.hasText(chainCode), PaymentTokenConfig::getChainCode, normalizeFilterCode(chainCode))
            .eq(StringUtils.hasText(tokenSymbol), PaymentTokenConfig::getTokenSymbol, normalizeFilterCode(tokenSymbol))
            .eq(enabled != null, PaymentTokenConfig::getEnabled, enabled)
            .orderByAsc(PaymentTokenConfig::getSortNo)
            .orderByAsc(PaymentTokenConfig::getId)));
  }

  /** 新增或更新 Token 配置。 */
  @Transactional
  public PaymentTokenConfig saveToken(TokenRequest request) {
    if (request == null || !StringUtils.hasText(request.chainCode())
        || !StringUtils.hasText(request.tokenSymbol())) {
      throw new IllegalArgumentException("chainCode and tokenSymbol are required");
    }
    PaymentTokenConfig existing = request.id() == null ? null : tokenConfigMapper.selectById(request.id());
    PaymentTokenConfig entity = existing == null ? new PaymentTokenConfig() : existing;
    entity.setId(request.id());
    if (existing == null) {
      entity.setChainCode(normalizeFilterCode(request.chainCode()));
      entity.setTokenSymbol(normalizeFilterCode(request.tokenSymbol()));
      entity.setTokenAddress(trimTextToNull(request.tokenAddress()));
    } else if (identityChanged(existing.getChainCode(), request.chainCode())
        || identityChanged(existing.getTokenSymbol(), request.tokenSymbol())
        || identityChanged(existing.getTokenAddress(), request.tokenAddress())) {
      throw new IllegalArgumentException("token identity fields cannot be changed after creation");
    }
    entity.setDecimals(request.decimals());
    entity.setConfirmationDepth(request.confirmationDepth());
    entity.setEnabled(request.enabled());
    entity.setSortNo(request.sortNo());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> tokenConfigMapper.insert(entity), () -> tokenConfigMapper.updateById(entity));
    return entity;
  }

  /** 代币接入向导：一次性保存链、代币、协议能力、扫描器和 Gas 策略。 */
  @Transactional(rollbackFor = Exception.class)
  public PaymentTokenConfig onboardToken(TokenOnboardingRequest request) {
    if (request == null || request.chain() == null || request.token() == null || request.capability() == null) {
      throw new IllegalArgumentException("chain, token and capability are required");
    }
    PaymentChainConfig chain = saveChainByCode(request.chain());
    TokenRequest tokenRequest = request.token();
    PaymentTokenConfig token = saveToken(new TokenRequest(
        null,
        chain.getChainCode(),
        tokenRequest.tokenSymbol(),
        tokenRequest.tokenAddress(),
        tokenRequest.decimals(),
        tokenRequest.confirmationDepth(),
        tokenRequest.enabled(),
        tokenRequest.sortNo()));
    saveTokenCapability(new TokenCapabilityRequest(
        chain.getChainCode(),
        token.getTokenSymbol(),
        request.capability().transferWithAuthorization(),
        request.capability().permit(),
        request.capability().approve(),
        request.capability().smartContractSettlement(),
        request.capability().settlementContractAddress(),
        request.capability().enabled()));
    if (request.scanner() != null) {
      ScannerRequest scanner = request.scanner();
      PaymentScannerConfig existingScanner = findScannerConfig(chain.getChainCode());
      saveScanner(new ScannerRequest(
          existingScanner == null ? scanner.id() : existingScanner.getId(),
          chain.getChainCode(),
          scanner.confirmationDepth(),
          scanner.scanIntervalSeconds(),
          scanner.checkpointFlushBlocks(),
          scanner.backfillBlocks(),
          scanner.logScanBatchBlocks(),
          scanner.logScanRetryAttempts()));
    }
    if (request.gas() != null) {
      GasRequest gas = request.gas();
      PaymentGasConfig existingGas = findGasConfig(chain.getChainCode());
      saveGas(new GasRequest(
          existingGas == null ? gas.id() : existingGas.getId(),
          chain.getChainCode(),
          gas.gasEnabled(),
          gas.hostedWalletPreferPlatform(),
          gas.hostedWalletSponsorEnabled(),
          gas.customerBalancePrecheckEnabled(),
          gas.platformSponsoredGasLimit(),
          gas.customerGasLimit(),
          gas.freeTransferGasLimit()));
    }
    return token;
  }

  private PaymentChainConfig saveChainByCode(ChainRequest request) {
    if (request == null || !StringUtils.hasText(request.chainCode())) {
      throw new IllegalArgumentException("chainCode is required");
    }
    String chainCode = normalizeFilterCode(request.chainCode());
    PaymentChainConfig existing = chainConfigMapper.selectOne(
        Wrappers.<PaymentChainConfig>lambdaQuery()
            .eq(PaymentChainConfig::getChainCode, chainCode)
            .last("limit 1"));
    return saveChain(new ChainRequest(
        existing == null ? request.id() : existing.getId(),
        chainCode,
        request.rpcUrl(),
        request.wsUrl(),
        request.confirmationDepth(),
        request.enabled(),
        request.sortNo()));
  }

  private PaymentScannerConfig findScannerConfig(String configScope) {
    return scannerConfigMapper.selectOne(Wrappers.<PaymentScannerConfig>lambdaQuery()
        .eq(PaymentScannerConfig::getConfigScope, configScope)
        .last("limit 1"));
  }

  private PaymentGasConfig findGasConfig(String configScope) {
    return gasConfigMapper.selectOne(Wrappers.<PaymentGasConfig>lambdaQuery()
        .eq(PaymentGasConfig::getConfigScope, configScope)
        .last("limit 1"));
  }

  /** 新增或更新 Token 协议能力配置。 */
  public PaymentTokenCapability saveTokenCapability(TokenCapabilityRequest request) {
    if (request == null || !StringUtils.hasText(request.chainCode())
        || !StringUtils.hasText(request.tokenSymbol())) {
      throw new IllegalArgumentException("chainCode and tokenSymbol are required");
    }
    String chainCode = normalizeFilterCode(request.chainCode());
    String tokenSymbol = normalizeFilterCode(request.tokenSymbol());
    PaymentTokenCapability existing = tokenCapabilityMapper.selectOne(
        Wrappers.<PaymentTokenCapability>lambdaQuery()
            .eq(PaymentTokenCapability::getChainCode, chainCode)
            .eq(PaymentTokenCapability::getTokenSymbol, tokenSymbol)
            .last("limit 1"));
    PaymentTokenCapability entity = existing == null ? new PaymentTokenCapability() : existing;
    entity.setChainCode(chainCode);
    entity.setTokenSymbol(tokenSymbol);
    entity.setTransferWithAuthorization(Boolean.TRUE.equals(request.transferWithAuthorization()));
    entity.setPermit(Boolean.TRUE.equals(request.permit()));
    entity.setApprove(request.approve() == null || Boolean.TRUE.equals(request.approve()));
    entity.setSmartContractSettlement(request.smartContractSettlement() == null
        || Boolean.TRUE.equals(request.smartContractSettlement()));
    entity.setSettlementContractAddress(request.settlementContractAddress());
    entity.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> tokenCapabilityMapper.insert(entity), () -> tokenCapabilityMapper.updateById(entity));
    return entity;
  }

  /** 分页查询回调重试与死信配置。 */
  public ManagerPageResponse<PaymentCallbackConfig> pageCallbacks(long page, long size) {
    return page(callbackConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentCallbackConfig>lambdaQuery().orderByAsc(PaymentCallbackConfig::getId)));
  }

  /** 新增或更新回调重试与死信配置。 */
  @Transactional
  public PaymentCallbackConfig saveCallback(CallbackRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentCallbackConfig entity = new PaymentCallbackConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setRetryEnabled(request.retryEnabled());
    entity.setMaxAttempts(request.maxAttempts());
    entity.setInitialBackoffSeconds(request.initialBackoffSeconds());
    entity.setMaxBackoffSeconds(request.maxBackoffSeconds());
    entity.setRetentionDays(request.retentionDays());
    entity.setDeadLetterEnabled(request.deadLetterEnabled());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> callbackConfigMapper.insert(entity), () -> callbackConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询 KYT 全局配置。 */
  public ManagerPageResponse<PaymentKytConfig> pageKyt(long page, long size) {
    return page(kytConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentKytConfig>lambdaQuery().orderByAsc(PaymentKytConfig::getId)));
  }

  /** 新增或更新 KYT 全局配置。 */
  @Transactional
  public PaymentKytConfig saveKyt(KytRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentKytConfig entity = new PaymentKytConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setKytEnabled(request.kytEnabled());
    entity.setStrictMode(request.strictMode());
    entity.setReviewThreshold(request.reviewThreshold());
    entity.setRejectThreshold(request.rejectThreshold());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> kytConfigMapper.insert(entity), () -> kytConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询派生地址配置。 */
  public ManagerPageResponse<PaymentDerivedAddressConfig> pageDerivedAddresses(long page, long size) {
    return page(derivedAddressConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentDerivedAddressConfig>lambdaQuery().orderByAsc(PaymentDerivedAddressConfig::getId)));
  }

  /** 新增或更新派生地址生成策略。 */
  @Transactional
  public PaymentDerivedAddressConfig saveDerivedAddress(DerivedAddressRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentDerivedAddressConfig entity = new PaymentDerivedAddressConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setDerivedEnabled(request.derivedEnabled());
    entity.setReuseAddress(request.reuseAddress());
    entity.setReuseCooldownMinutes(request.reuseCooldownMinutes());
    entity.setMode(request.mode());
    entity.setHdMnemonicEnv(request.hdMnemonicEnv());
    entity.setHdPassphraseEnv(request.hdPassphraseEnv());
    entity.setHdDerivationPathPrefix(request.hdDerivationPathPrefix());
    entity.setHdStartIndex(request.hdStartIndex());
    entity.setHdGenerateMnemonicWhenMissing(request.hdGenerateMnemonicWhenMissing());
    entity.setKeystoreOutputDir(request.keystoreOutputDir());
    entity.setKeystorePasswordEnv(request.keystorePasswordEnv());
    entity.setKeystoreFilePrefix(request.keystoreFilePrefix());
    entity.setThirdPartyEnabled(request.thirdPartyEnabled());
    entity.setThirdPartyBaseUrl(request.thirdPartyBaseUrl());
    entity.setThirdPartyCreatePath(request.thirdPartyCreatePath());
    entity.setThirdPartyApiKeyEnv(request.thirdPartyApiKeyEnv());
    entity.setThirdPartyProviderName(request.thirdPartyProviderName());
    entity.setAddressFactoryNamespace(request.addressFactoryNamespace());
    entity.setAddressFactorySeedPrefix(request.addressFactorySeedPrefix());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> derivedAddressConfigMapper.insert(entity), () -> derivedAddressConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询扫描器运行策略配置。 */
  public ManagerPageResponse<PaymentScannerConfig> pageScanners(long page, long size) {
    return page(scannerConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentScannerConfig>lambdaQuery().orderByAsc(PaymentScannerConfig::getId)));
  }

  /** 新增或更新扫描器运行策略配置。 */
  @Transactional
  public PaymentScannerConfig saveScanner(ScannerRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentScannerConfig entity = new PaymentScannerConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setConfirmationDepth(request.confirmationDepth());
    entity.setScanIntervalSeconds(request.scanIntervalSeconds());
    entity.setCheckpointFlushBlocks(request.checkpointFlushBlocks());
    entity.setBackfillBlocks(request.backfillBlocks());
    entity.setLogScanBatchBlocks(request.logScanBatchBlocks());
    entity.setLogScanRetryAttempts(request.logScanRetryAttempts());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> scannerConfigMapper.insert(entity), () -> scannerConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询 Gas 预检和代付策略配置。 */
  public ManagerPageResponse<PaymentGasConfig> pageGas(long page, long size) {
    return page(gasConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentGasConfig>lambdaQuery().orderByAsc(PaymentGasConfig::getId)));
  }

  /** 新增或更新 Gas 预检和代付策略配置。 */
  @Transactional
  public PaymentGasConfig saveGas(GasRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentGasConfig entity = new PaymentGasConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setGasEnabled(request.gasEnabled());
    entity.setHostedWalletPreferPlatform(request.hostedWalletPreferPlatform());
    entity.setHostedWalletSponsorEnabled(request.hostedWalletSponsorEnabled());
    entity.setCustomerBalancePrecheckEnabled(request.customerBalancePrecheckEnabled());
    entity.setPlatformSponsoredGasLimit(request.platformSponsoredGasLimit());
    entity.setCustomerGasLimit(request.customerGasLimit());
    entity.setFreeTransferGasLimit(request.freeTransferGasLimit());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> gasConfigMapper.insert(entity), () -> gasConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询 x402 网关与服务费配置。 */
  public ManagerPageResponse<PaymentGatewayConfig> pageGateways(long page, long size) {
    return page(gatewayConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentGatewayConfig>lambdaQuery().orderByAsc(PaymentGatewayConfig::getId)));
  }

  /** 新增或更新 x402 网关与服务费配置。 */
  @Transactional
  public PaymentGatewayConfig saveGateway(GatewayRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentGatewayConfig entity = new PaymentGatewayConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setGatewayEnabled(request.gatewayEnabled());
    entity.setServiceFeeToken(request.serviceFeeToken());
    entity.setServiceFeeAmount(request.serviceFeeAmount());
    entity.setApiKey(request.apiKey());
    entity.setPublicResourcesEnabled(request.publicResourcesEnabled());
    entity.setFacilitatorName(request.facilitatorName());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> gatewayConfigMapper.insert(entity), () -> gatewayConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询订阅支付配置。 */
  public ManagerPageResponse<PaymentSubscriptionConfig> pageSubscriptions(long page, long size) {
    return page(subscriptionConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentSubscriptionConfig>lambdaQuery().orderByAsc(PaymentSubscriptionConfig::getId)));
  }

  /** 新增或更新订阅支付配置。 */
  @Transactional
  public PaymentSubscriptionConfig saveSubscription(SubscriptionRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentSubscriptionConfig entity = new PaymentSubscriptionConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setSubscriptionEnabled(request.subscriptionEnabled());
    entity.setDefaultMode(request.defaultMode());
    entity.setDefaultCycleSeconds(request.defaultCycleSeconds());
    entity.setSuperfluidHostAddress(request.superfluidHostAddress());
    entity.setSuperfluidCfaAddress(request.superfluidCfaAddress());
    entity.setErc1337ExecutorAddress(request.erc1337ExecutorAddress());
    entity.setSchedulerEnabled(request.schedulerEnabled());
    entity.setSchedulerBatchSize(request.schedulerBatchSize());
    entity.setMaxRetryCount(request.maxRetryCount());
    entity.setRetryBackoffSeconds(request.retryBackoffSeconds());
    entity.setExecutionGasLimit(request.executionGasLimit());
    entity.setExecutorPrivateKeySourceType(request.executorPrivateKeySourceType());
    entity.setExecutorPrivateKeyEnv(request.executorPrivateKeyEnv());
    entity.setExecutorPrivateKeyKmsKeyId(request.executorPrivateKeyKmsKeyId());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> subscriptionConfigMapper.insert(entity), () -> subscriptionConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询平台基础配置。 */
  public ManagerPageResponse<PaymentPlatformConfig> pagePlatforms(long page, long size) {
    return page(platformConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentPlatformConfig>lambdaQuery().orderByAsc(PaymentPlatformConfig::getId)));
  }

  /** 新增或更新平台基础配置。 */
  @Transactional
  public PaymentPlatformConfig savePlatform(PlatformRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentPlatformConfig entity = new PaymentPlatformConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setOrderExpireMinutes(request.orderExpireMinutes());
    entity.setCashierBaseUrl(request.cashierBaseUrl());
    entity.setTreasuryAddress(request.treasuryAddress());
    entity.setIdempotencyWaitMillis(request.idempotencyWaitMillis());
    entity.setEnabled(request.enabled());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> platformConfigMapper.insert(entity), () -> platformConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询收银台 Token 配置。 */
  public ManagerPageResponse<PaymentCashierConfig> pageCashiers(long page, long size) {
    return page(cashierConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentCashierConfig>lambdaQuery().orderByAsc(PaymentCashierConfig::getId)));
  }

  /** 新增或更新收银台 Token 配置。 */
  @Transactional
  public PaymentCashierConfig saveCashier(CashierRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentCashierConfig entity = new PaymentCashierConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setTokenEncryptionEnabled(request.tokenEncryptionEnabled());
    entity.setTokenKeyAlias(request.tokenKeyAlias());
    entity.setTokenTtlMinutes(request.tokenTtlMinutes());
    entity.setTokenPrefix(request.tokenPrefix());
    entity.setShortTokenEnabled(request.shortTokenEnabled());
    entity.setShortTokenPrefix(request.shortTokenPrefix());
    entity.setShortTokenLength(request.shortTokenLength());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> cashierConfigMapper.insert(entity), () -> cashierConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询安全策略配置。 */
  public ManagerPageResponse<PaymentSecurityConfig> pageSecurity(long page, long size) {
    return page(securityConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentSecurityConfig>lambdaQuery().orderByAsc(PaymentSecurityConfig::getId)));
  }

  /** 新增或更新开放重定向等安全策略。 */
  @Transactional
  public PaymentSecurityConfig saveSecurity(SecurityRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentSecurityConfig entity = new PaymentSecurityConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setValidateRedirectUrl(request.validateRedirectUrl());
    entity.setAllowLocalRedirect(request.allowLocalRedirect());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> securityConfigMapper.insert(entity), () -> securityConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询 discovery 能力自描述配置。 */
  public ManagerPageResponse<PaymentDiscoveryConfig> pageDiscovery(long page, long size) {
    return page(discoveryConfigMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentDiscoveryConfig>lambdaQuery().orderByAsc(PaymentDiscoveryConfig::getId)));
  }

  /** 新增或更新 discovery 能力自描述配置。 */
  @Transactional
  public PaymentDiscoveryConfig saveDiscovery(DiscoveryRequest request) {
    requireScope(request == null ? null : request.configScope());
    PaymentDiscoveryConfig entity = new PaymentDiscoveryConfig();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setDiscoveryEnabled(request.discoveryEnabled());
    entity.setPublicBaseUrl(request.publicBaseUrl());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> discoveryConfigMapper.insert(entity), () -> discoveryConfigMapper.updateById(entity));
    return entity;
  }

  /** 分页查询合约分账规则配置。 */
  public ManagerPageResponse<PaymentContractSplitRule> pageSplitRules(
      long page, long size, String configScope, Boolean enabled) {
    return page(splitRuleMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<PaymentContractSplitRule>lambdaQuery()
            .eq(StringUtils.hasText(configScope), PaymentContractSplitRule::getConfigScope, configScope)
            .eq(enabled != null, PaymentContractSplitRule::getEnabled, enabled)
            .orderByAsc(PaymentContractSplitRule::getSortNo)
            .orderByAsc(PaymentContractSplitRule::getId)));
  }

  /** 新增或更新合约分账规则配置。 */
  @Transactional
  public PaymentContractSplitRule saveSplitRule(ContractSplitRuleRequest request) {
    requireScope(request == null ? null : request.configScope());
    if (!StringUtils.hasText(request.roleCode()) || !StringUtils.hasText(request.receiverAddress())) {
      throw new IllegalArgumentException("roleCode and receiverAddress are required");
    }
    PaymentContractSplitRule entity = new PaymentContractSplitRule();
    entity.setId(request.id());
    entity.setConfigScope(request.configScope());
    entity.setRoleCode(request.roleCode());
    entity.setReceiverAddress(request.receiverAddress());
    entity.setBasisPoints(request.basisPoints());
    entity.setSortNo(request.sortNo());
    entity.setEnabled(request.enabled());
    touchAndSave(entity.getId(), entity::setCreatedAt, entity::setUpdatedAt,
        () -> splitRuleMapper.insert(entity), () -> splitRuleMapper.updateById(entity));
    return entity;
  }

  /** 校验配置作用域不能为空。 */
  private void requireScope(String configScope) {
    if (!StringUtils.hasText(configScope)) {
      throw new IllegalArgumentException("configScope is required");
    }
  }

  private boolean identityChanged(String existing, String requested) {
    return StringUtils.hasText(requested) && !same(existing, requested);
  }

  private boolean same(String left, String right) {
    String normalizedLeft = left == null ? "" : left.trim();
    String normalizedRight = right == null ? "" : right.trim();
    return normalizedLeft.equalsIgnoreCase(normalizedRight);
  }

}
