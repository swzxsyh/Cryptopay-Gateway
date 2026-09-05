package io.swzxsyh.manager.api.dto;

import java.math.BigDecimal;

/** 运行配置管理相关 DTO 聚合，避免每个配置请求都单独占一个文件。 */
public final class ManagerConfigDtos {

  private ManagerConfigDtos() {}

  /** 支付链配置保存请求。 */
  public record ChainRequest(
      Long id,
      String chainCode,
      String rpcUrl,
      String wsUrl,
      Integer confirmationDepth,
      Boolean enabled,
      Integer sortNo) {}

  /** Token 配置保存请求。 */
  public record TokenRequest(
      Long id,
      String chainCode,
      String tokenSymbol,
      String tokenAddress,
      Integer decimals,
      Integer confirmationDepth,
      Boolean enabled,
      Integer sortNo) {}

  /** 新增代币接入向导请求，链、代币、协议能力、扫描器、Gas 配置会在同一事务内保存。 */
  public record TokenOnboardingRequest(
      ChainRequest chain,
      TokenRequest token,
      TokenCapabilityRequest capability,
      ScannerRequest scanner,
      GasRequest gas) {}

  /** Token 协议能力保存请求。 */
  public record TokenCapabilityRequest(
      String chainCode,
      String tokenSymbol,
      Boolean transferWithAuthorization,
      Boolean permit,
      Boolean approve,
      Boolean smartContractSettlement,
      String settlementContractAddress,
      Boolean enabled) {}

  /** 回调重试配置保存请求。 */
  public record CallbackRequest(
      Long id,
      String configScope,
      Boolean retryEnabled,
      Integer maxAttempts,
      Long initialBackoffSeconds,
      Long maxBackoffSeconds,
      Long retentionDays,
      Boolean deadLetterEnabled) {}

  /** KYT 配置保存请求。 */
  public record KytRequest(
      Long id,
      String configScope,
      Boolean kytEnabled,
      Boolean strictMode,
      Integer reviewThreshold,
      Integer rejectThreshold) {}

  /** 派生地址策略保存请求。 */
  public record DerivedAddressRequest(
      Long id,
      String configScope,
      Boolean derivedEnabled,
      Boolean reuseAddress,
      Integer reuseCooldownMinutes,
      String mode,
      String hdMnemonicEnv,
      String hdPassphraseEnv,
      String hdDerivationPathPrefix,
      Integer hdStartIndex,
      Boolean hdGenerateMnemonicWhenMissing,
      String keystoreOutputDir,
      String keystorePasswordEnv,
      String keystoreFilePrefix,
      Boolean thirdPartyEnabled,
      String thirdPartyBaseUrl,
      String thirdPartyCreatePath,
      String thirdPartyApiKeyEnv,
      String thirdPartyProviderName,
      String addressFactoryNamespace,
      String addressFactorySeedPrefix) {}

  /** 扫描器配置保存请求。 */
  public record ScannerRequest(
      Long id,
      String configScope,
      Integer confirmationDepth,
      Integer scanIntervalSeconds,
      Integer checkpointFlushBlocks,
      Integer backfillBlocks,
      Integer logScanBatchBlocks,
      Integer logScanRetryAttempts) {}

  /** Gas 策略配置保存请求。 */
  public record GasRequest(
      Long id,
      String configScope,
      Boolean gasEnabled,
      Boolean hostedWalletPreferPlatform,
      Boolean hostedWalletSponsorEnabled,
      Boolean customerBalancePrecheckEnabled,
      Integer platformSponsoredGasLimit,
      Integer customerGasLimit,
      Integer freeTransferGasLimit) {}

  /** 网关配置保存请求。 */
  public record GatewayRequest(
      Long id,
      String configScope,
      Boolean gatewayEnabled,
      String serviceFeeToken,
      BigDecimal serviceFeeAmount,
      String apiKey,
      Boolean publicResourcesEnabled,
      String facilitatorName) {}

  /** 订阅支付配置保存请求。 */
  public record SubscriptionRequest(
      Long id,
      String configScope,
      Boolean subscriptionEnabled,
      String defaultMode,
      Integer defaultCycleSeconds,
      String superfluidHostAddress,
      String superfluidCfaAddress,
      String erc1337ExecutorAddress,
      Boolean schedulerEnabled,
      Integer schedulerBatchSize,
      Integer maxRetryCount,
      Integer retryBackoffSeconds,
      java.math.BigInteger executionGasLimit,
      String executorPrivateKeySourceType,
      String executorPrivateKeyEnv,
      String executorPrivateKeyKmsKeyId) {}

  /** 平台基础配置保存请求。 */
  public record PlatformRequest(
      Long id,
      String configScope,
      Integer orderExpireMinutes,
      String cashierBaseUrl,
      String treasuryAddress,
      Long idempotencyWaitMillis,
      Boolean enabled) {}

  /** 收银台 Token 配置保存请求。 */
  public record CashierRequest(
      Long id,
      String configScope,
      Boolean tokenEncryptionEnabled,
      String tokenKeyAlias,
      Integer tokenTtlMinutes,
      String tokenPrefix,
      Boolean shortTokenEnabled,
      String shortTokenPrefix,
      Integer shortTokenLength) {}

  /** 安全策略配置保存请求。 */
  public record SecurityRequest(
      Long id,
      String configScope,
      Boolean validateRedirectUrl,
      Boolean allowLocalRedirect) {}

  /** 能力发现配置保存请求。 */
  public record DiscoveryRequest(
      Long id,
      String configScope,
      Boolean discoveryEnabled,
      String publicBaseUrl) {}

  /** 合约分账规则保存请求。 */
  public record ContractSplitRuleRequest(
      Long id,
      String configScope,
      String roleCode,
      String receiverAddress,
      Integer basisPoints,
      Integer sortNo,
      Boolean enabled) {}
}
