package io.swzxsyh.payment.config;

import io.swzxsyh.payment.util.SecretSourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 支付模块的统一配置入口。 */
@Component
@ConfigurationProperties(prefix = "crypto.payment")
public class CryptoPaymentProperties {

  private int orderExpireMinutes = 15;
  private String cashierBaseUrl = "http://localhost:9888/crypto-gateway";
  private String treasuryAddress = "0x0000000000000000000000000000000000000000";
  private long idempotencyWaitMillis = 4000;
  private Contract contract = new Contract();
  private DerivedAddress derivedAddress = new DerivedAddress();
  private Gateway gateway = new Gateway();
  private Discovery discovery = new Discovery();
  private Gas gas = new Gas();
  private Redis redis = new Redis();
  private Kyt kyt = new Kyt();
  private Scanner scanner = new Scanner();
  private Map<String, Scanner> chainScannerProfiles = new LinkedHashMap<>();
  private Subscription subscription = new Subscription();
  private Signature signature = new Signature();
  private Callback callback = new Callback();
  private Security security = new Security();
  private Cashier cashier = new Cashier();
  private Secrets secrets = new Secrets();
  private Messaging messaging = new Messaging();
  private ExternalHttp externalHttp = new ExternalHttp();
  private Solana solana = new Solana();
  private List<ChainProfile> chainProfiles = new ArrayList<>();
  private List<TokenProfile> tokenProfiles = new ArrayList<>();

  public int getOrderExpireMinutes() {
    return orderExpireMinutes;
  }

  public void setOrderExpireMinutes(int orderExpireMinutes) {
    this.orderExpireMinutes = orderExpireMinutes;
  }

  public String getCashierBaseUrl() {
    return cashierBaseUrl;
  }

  public void setCashierBaseUrl(String cashierBaseUrl) {
    this.cashierBaseUrl = cashierBaseUrl;
  }

  public String getTreasuryAddress() {
    return treasuryAddress;
  }

  public void setTreasuryAddress(String treasuryAddress) {
    this.treasuryAddress = treasuryAddress;
  }

  public long getIdempotencyWaitMillis() {
    return idempotencyWaitMillis;
  }

  public void setIdempotencyWaitMillis(long idempotencyWaitMillis) {
    this.idempotencyWaitMillis = idempotencyWaitMillis;
  }

  public Contract getContract() {
    return contract;
  }

  public void setContract(Contract contract) {
    this.contract = contract;
  }

  public DerivedAddress getDerivedAddress() {
    return derivedAddress;
  }

  public void setDerivedAddress(DerivedAddress derivedAddress) {
    this.derivedAddress = derivedAddress;
  }

  public Gateway getGateway() {
    return gateway;
  }

  public void setGateway(Gateway gateway) {
    this.gateway = gateway;
  }

  public Discovery getDiscovery() {
    return discovery;
  }

  public void setDiscovery(Discovery discovery) {
    this.discovery = discovery;
  }

  public Gas getGas() {
    return gas;
  }

  public void setGas(Gas gas) {
    this.gas = gas;
  }

  public Redis getRedis() {
    return redis;
  }

  public void setRedis(Redis redis) {
    this.redis = redis;
  }

  public Kyt getKyt() {
    return kyt;
  }

  public void setKyt(Kyt kyt) {
    this.kyt = kyt;
  }

  public Scanner getScanner() {
    return scanner;
  }

  public void setScanner(Scanner scanner) {
    this.scanner = scanner;
  }

  public Map<String, Scanner> getChainScannerProfiles() {
    return chainScannerProfiles;
  }

  public void setChainScannerProfiles(Map<String, Scanner> chainScannerProfiles) {
    this.chainScannerProfiles = chainScannerProfiles;
  }

  /** 获取指定链的扫描配置；单链没有配置时回落到全局配置。 */
  public Scanner scannerForChain(String chain) {
    if (chain == null || chainScannerProfiles == null || chainScannerProfiles.isEmpty()) {
      return scanner;
    }
    Scanner chainScanner = chainScannerProfiles.get(chain.trim().toUpperCase());
    return chainScanner == null ? scanner : chainScanner;
  }

  public Subscription getSubscription() {
    return subscription;
  }

  public void setSubscription(Subscription subscription) {
    this.subscription = subscription;
  }

  public Signature getSignature() {
    return signature;
  }

  public void setSignature(Signature signature) {
    this.signature = signature;
  }

  public Callback getCallback() {
    return callback;
  }

  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  public Security getSecurity() {
    return security;
  }

  public void setSecurity(Security security) {
    this.security = security;
  }

  public Cashier getCashier() {
    return cashier;
  }

  public void setCashier(Cashier cashier) {
    this.cashier = cashier;
  }

  public Secrets getSecrets() {
    return secrets;
  }

  public void setSecrets(Secrets secrets) {
    this.secrets = secrets;
  }

  public Messaging getMessaging() {
    return messaging;
  }

  public void setMessaging(Messaging messaging) {
    this.messaging = messaging;
  }

  public ExternalHttp getExternalHttp() {
    return externalHttp;
  }

  public void setExternalHttp(ExternalHttp externalHttp) {
    this.externalHttp = externalHttp;
  }

  public Solana getSolana() {
    return solana;
  }

  public void setSolana(Solana solana) {
    this.solana = solana;
  }

  public List<TokenProfile> getTokenProfiles() {
    return tokenProfiles;
  }

  public void setTokenProfiles(List<TokenProfile> tokenProfiles) {
    this.tokenProfiles = tokenProfiles;
  }

  public List<ChainProfile> getChainProfiles() {
    return chainProfiles;
  }

  public void setChainProfiles(List<ChainProfile> chainProfiles) {
    this.chainProfiles = chainProfiles;
  }

  public static class Contract {

    private boolean enabled = true;
    private String address = "0x0000000000000000000000000000000000000000";
    private boolean create2Enabled = false;
    private boolean create2HostedWalletOnly = true;
    private String create2FactoryAddress = "";
    private String create2InitCodeHash = "";
    private String create2SaltPrefix = "crypto:payment:escrow";
    private List<SettlementRule> settlementRules = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getAddress() {
      return address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public boolean isCreate2Enabled() {
      return create2Enabled;
    }

    public void setCreate2Enabled(boolean create2Enabled) {
      this.create2Enabled = create2Enabled;
    }

    public boolean isCreate2HostedWalletOnly() {
      return create2HostedWalletOnly;
    }

    public void setCreate2HostedWalletOnly(boolean create2HostedWalletOnly) {
      this.create2HostedWalletOnly = create2HostedWalletOnly;
    }

    public String getCreate2FactoryAddress() {
      return create2FactoryAddress;
    }

    public void setCreate2FactoryAddress(String create2FactoryAddress) {
      this.create2FactoryAddress = create2FactoryAddress;
    }

    public String getCreate2InitCodeHash() {
      return create2InitCodeHash;
    }

    public void setCreate2InitCodeHash(String create2InitCodeHash) {
      this.create2InitCodeHash = create2InitCodeHash;
    }

    public String getCreate2SaltPrefix() {
      return create2SaltPrefix;
    }

    public void setCreate2SaltPrefix(String create2SaltPrefix) {
      this.create2SaltPrefix = create2SaltPrefix;
    }

    public List<SettlementRule> getSettlementRules() {
      return settlementRules;
    }

    public void setSettlementRules(List<SettlementRule> settlementRules) {
      this.settlementRules = settlementRules;
    }
  }

  public static class SettlementRule {

    private String role;
    private String receiver;
    private int basisPoints;

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

    public String getReceiver() {
      return receiver;
    }

    public void setReceiver(String receiver) {
      this.receiver = receiver;
    }

    public int getBasisPoints() {
      return basisPoints;
    }

    public void setBasisPoints(int basisPoints) {
      this.basisPoints = basisPoints;
    }
  }

  public static class DerivedAddress {

    private boolean enabled = true;
    private boolean reuseAddress = false;
    private int reuseCooldownMinutes = 60;
    private String mode = "ADDRESS_FACTORY";
    private List<String> pool = new ArrayList<>();
    private Hd hd = new Hd();
    private Keystore keystore = new Keystore();
    private ThirdPartyApi thirdPartyApi = new ThirdPartyApi();
    private AddressFactory addressFactory = new AddressFactory();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isReuseAddress() {
      return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
      this.reuseAddress = reuseAddress;
    }

    public int getReuseCooldownMinutes() {
      return reuseCooldownMinutes;
    }

    public void setReuseCooldownMinutes(int reuseCooldownMinutes) {
      this.reuseCooldownMinutes = reuseCooldownMinutes;
    }

    public String getMode() {
      return mode;
    }

    public void setMode(String mode) {
      this.mode = mode;
    }

    public List<String> getPool() {
      return pool;
    }

    public void setPool(List<String> pool) {
      this.pool = pool;
    }

    public Hd getHd() {
      return hd;
    }

    public void setHd(Hd hd) {
      this.hd = hd;
    }

    public Keystore getKeystore() {
      return keystore;
    }

    public void setKeystore(Keystore keystore) {
      this.keystore = keystore;
    }

    public ThirdPartyApi getThirdPartyApi() {
      return thirdPartyApi;
    }

    public void setThirdPartyApi(ThirdPartyApi thirdPartyApi) {
      this.thirdPartyApi = thirdPartyApi;
    }

    public AddressFactory getAddressFactory() {
      return addressFactory;
    }

    public void setAddressFactory(AddressFactory addressFactory) {
      this.addressFactory = addressFactory;
    }

    public static class Hd {

      private SecretSourceType mnemonicSourceType = SecretSourceType.ENV;
      private String mnemonic;
      private String mnemonicEnv = "CRYPTO_PAYMENT_DERIVED_HD_MNEMONIC";
      private String mnemonicKmsKeyId;
      private String passphrase = "";
      private String derivationPathPrefix = "m/44'/60'/0'/0";
      private int startIndex = 0;
      private boolean generateMnemonicWhenMissing = true;

      public SecretSourceType getMnemonicSourceType() {
        return mnemonicSourceType;
      }

      public void setMnemonicSourceType(SecretSourceType mnemonicSourceType) {
        this.mnemonicSourceType = mnemonicSourceType;
      }

      public String getMnemonic() {
        return mnemonic;
      }

      public void setMnemonic(String mnemonic) {
        this.mnemonic = mnemonic;
      }

      public String getMnemonicEnv() {
        return mnemonicEnv;
      }

      public void setMnemonicEnv(String mnemonicEnv) {
        this.mnemonicEnv = mnemonicEnv;
      }

      public String getMnemonicKmsKeyId() {
        return mnemonicKmsKeyId;
      }

      public void setMnemonicKmsKeyId(String mnemonicKmsKeyId) {
        this.mnemonicKmsKeyId = mnemonicKmsKeyId;
      }

      public String getPassphrase() {
        return passphrase;
      }

      public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
      }

      public String getDerivationPathPrefix() {
        return derivationPathPrefix;
      }

      public void setDerivationPathPrefix(String derivationPathPrefix) {
        this.derivationPathPrefix = derivationPathPrefix;
      }

      public int getStartIndex() {
        return startIndex;
      }

      public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
      }

      public boolean isGenerateMnemonicWhenMissing() {
        return generateMnemonicWhenMissing;
      }

      public void setGenerateMnemonicWhenMissing(boolean generateMnemonicWhenMissing) {
        this.generateMnemonicWhenMissing = generateMnemonicWhenMissing;
      }
    }

    public static class Keystore {

      private SecretSourceType passwordSourceType = SecretSourceType.ENV;
      private String outputDir = "./wallet-keys";
      private String password;
      private String passwordEnv = "CRYPTO_PAYMENT_DERIVED_KEYSTORE_PASSWORD";
      private String passwordKmsKeyId;
      private String filePrefix = "crypto-wallet";

      public SecretSourceType getPasswordSourceType() {
        return passwordSourceType;
      }

      public void setPasswordSourceType(SecretSourceType passwordSourceType) {
        this.passwordSourceType = passwordSourceType;
      }

      public String getOutputDir() {
        return outputDir;
      }

      public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
      }

      public String getPassword() {
        return password;
      }

      public void setPassword(String password) {
        this.password = password;
      }

      public String getPasswordEnv() {
        return passwordEnv;
      }

      public void setPasswordEnv(String passwordEnv) {
        this.passwordEnv = passwordEnv;
      }

      public String getPasswordKmsKeyId() {
        return passwordKmsKeyId;
      }

      public void setPasswordKmsKeyId(String passwordKmsKeyId) {
        this.passwordKmsKeyId = passwordKmsKeyId;
      }

      public String getFilePrefix() {
        return filePrefix;
      }

      public void setFilePrefix(String filePrefix) {
        this.filePrefix = filePrefix;
      }
    }

    public static class ThirdPartyApi {

      private SecretSourceType apiKeySourceType = SecretSourceType.ENV;
      private boolean enabled = false;
      private String baseUrl = "";
      private String createPath = "/wallets/derived";
      private String apiKey = "";
      private String apiKeyEnv = "CRYPTO_PAYMENT_DERIVED_THIRD_PARTY_API_KEY";
      private String apiKeyKmsKeyId;
      private String providerName = "external-wallet-provider";

      public SecretSourceType getApiKeySourceType() {
        return apiKeySourceType;
      }

      public void setApiKeySourceType(SecretSourceType apiKeySourceType) {
        this.apiKeySourceType = apiKeySourceType;
      }

      public boolean isEnabled() {
        return enabled;
      }

      public void setEnabled(boolean enabled) {
        this.enabled = enabled;
      }

      public String getBaseUrl() {
        return baseUrl;
      }

      public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
      }

      public String getCreatePath() {
        return createPath;
      }

      public void setCreatePath(String createPath) {
        this.createPath = createPath;
      }

      public String getApiKey() {
        return apiKey;
      }

      public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
      }

      public String getApiKeyEnv() {
        return apiKeyEnv;
      }

      public void setApiKeyEnv(String apiKeyEnv) {
        this.apiKeyEnv = apiKeyEnv;
      }

      public String getApiKeyKmsKeyId() {
        return apiKeyKmsKeyId;
      }

      public void setApiKeyKmsKeyId(String apiKeyKmsKeyId) {
        this.apiKeyKmsKeyId = apiKeyKmsKeyId;
      }

      public String getProviderName() {
        return providerName;
      }

      public void setProviderName(String providerName) {
        this.providerName = providerName;
      }
    }

    public static class AddressFactory {

      private String namespace = "swzxsyh";
      private String seedPrefix = "derived";

      public String getNamespace() {
        return namespace;
      }

      public void setNamespace(String namespace) {
        this.namespace = namespace;
      }

      public String getSeedPrefix() {
        return seedPrefix;
      }

      public void setSeedPrefix(String seedPrefix) {
        this.seedPrefix = seedPrefix;
      }
    }
  }

  public static class TokenProfile {

    private String chain;
    private String token;
    private String tokenAddress;
    private int decimals = 6;
    private Integer confirmationDepth;
    private boolean transferWithAuthorization;
    private boolean permit;
    private boolean approve = true;
    private boolean smartContractSettlement = true;
    private String settlementContractAddress;

    public String getChain() {
      return chain;
    }

    public void setChain(String chain) {
      this.chain = chain;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getTokenAddress() {
      return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
      this.tokenAddress = tokenAddress;
    }

    public int getDecimals() {
      return decimals;
    }

    public void setDecimals(int decimals) {
      this.decimals = decimals;
    }

    public Integer getConfirmationDepth() {
      return confirmationDepth;
    }

    public void setConfirmationDepth(Integer confirmationDepth) {
      this.confirmationDepth = confirmationDepth;
    }

    public boolean isTransferWithAuthorization() {
      return transferWithAuthorization;
    }

    public void setTransferWithAuthorization(boolean transferWithAuthorization) {
      this.transferWithAuthorization = transferWithAuthorization;
    }

    public boolean isPermit() {
      return permit;
    }

    public void setPermit(boolean permit) {
      this.permit = permit;
    }

    public boolean isApprove() {
      return approve;
    }

    public void setApprove(boolean approve) {
      this.approve = approve;
    }

    public boolean isSmartContractSettlement() {
      return smartContractSettlement;
    }

    public void setSmartContractSettlement(boolean smartContractSettlement) {
      this.smartContractSettlement = smartContractSettlement;
    }

    public String getSettlementContractAddress() {
      return settlementContractAddress;
    }

    public void setSettlementContractAddress(String settlementContractAddress) {
      this.settlementContractAddress = settlementContractAddress;
    }
  }

  public static class ChainProfile {

    private String chain;
    private String rpcUrl;
    private String wsUrl;
    private Integer confirmationDepth;
    private Boolean sponsorEnabled;
    private String sponsorProvider = "AUTO";
    private String relayerAddress;
    private Long sponsorGasLimit;
    private boolean enabled = true;

    public String getChain() {
      return chain;
    }

    public void setChain(String chain) {
      this.chain = chain;
    }

    public String getRpcUrl() {
      return rpcUrl;
    }

    public void setRpcUrl(String rpcUrl) {
      this.rpcUrl = rpcUrl;
    }

    public String getWsUrl() {
      return wsUrl;
    }

    public void setWsUrl(String wsUrl) {
      this.wsUrl = wsUrl;
    }

    public Integer getConfirmationDepth() {
      return confirmationDepth;
    }

    public void setConfirmationDepth(Integer confirmationDepth) {
      this.confirmationDepth = confirmationDepth;
    }

    public Boolean getSponsorEnabled() {
      return sponsorEnabled;
    }

    public void setSponsorEnabled(Boolean sponsorEnabled) {
      this.sponsorEnabled = sponsorEnabled;
    }

    public String getSponsorProvider() {
      return sponsorProvider;
    }

    public void setSponsorProvider(String sponsorProvider) {
      this.sponsorProvider = sponsorProvider;
    }

    public String getRelayerAddress() {
      return relayerAddress;
    }

    public void setRelayerAddress(String relayerAddress) {
      this.relayerAddress = relayerAddress;
    }

    public Long getSponsorGasLimit() {
      return sponsorGasLimit;
    }

    public void setSponsorGasLimit(Long sponsorGasLimit) {
      this.sponsorGasLimit = sponsorGasLimit;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  public static class Gateway {

    private boolean enabled = true;
    private String serviceFeeToken = "USDC";
    private java.math.BigDecimal serviceFeeAmount = new java.math.BigDecimal("0.10");
    private String apiKey = "";
    private boolean publicResourcesEnabled = true;
    private String facilitatorName = "CryptoPay GateWay X402 Facilitator";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getServiceFeeToken() {
      return serviceFeeToken;
    }

    public void setServiceFeeToken(String serviceFeeToken) {
      this.serviceFeeToken = serviceFeeToken;
    }

    public java.math.BigDecimal getServiceFeeAmount() {
      return serviceFeeAmount;
    }

    public void setServiceFeeAmount(java.math.BigDecimal serviceFeeAmount) {
      this.serviceFeeAmount = serviceFeeAmount;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public boolean isPublicResourcesEnabled() {
      return publicResourcesEnabled;
    }

    public void setPublicResourcesEnabled(boolean publicResourcesEnabled) {
      this.publicResourcesEnabled = publicResourcesEnabled;
    }

    public String getFacilitatorName() {
      return facilitatorName;
    }

    public void setFacilitatorName(String facilitatorName) {
      this.facilitatorName = facilitatorName;
    }
  }

  public static class Discovery {

    private boolean enabled = true;
    private String publicBaseUrl = "http://localhost:8080";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getPublicBaseUrl() {
      return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
      this.publicBaseUrl = publicBaseUrl;
    }
  }

  public static class Kyt {

    private boolean enabled = true;
    private boolean strictMode = false;
    private int reviewThreshold = 60;
    private int rejectThreshold = 80;
    private List<String> allowAddresses = new ArrayList<>();
    private List<String> denyAddresses = new ArrayList<>();
    private List<String> highRiskChains = new ArrayList<>();
    private List<String> highRiskTokens = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isStrictMode() {
      return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
      this.strictMode = strictMode;
    }

    public int getReviewThreshold() {
      return reviewThreshold;
    }

    public void setReviewThreshold(int reviewThreshold) {
      this.reviewThreshold = reviewThreshold;
    }

    public int getRejectThreshold() {
      return rejectThreshold;
    }

    public void setRejectThreshold(int rejectThreshold) {
      this.rejectThreshold = rejectThreshold;
    }

    public List<String> getAllowAddresses() {
      return allowAddresses;
    }

    public void setAllowAddresses(List<String> allowAddresses) {
      this.allowAddresses = allowAddresses;
    }

    public List<String> getDenyAddresses() {
      return denyAddresses;
    }

    public void setDenyAddresses(List<String> denyAddresses) {
      this.denyAddresses = denyAddresses;
    }

    public List<String> getHighRiskChains() {
      return highRiskChains;
    }

    public void setHighRiskChains(List<String> highRiskChains) {
      this.highRiskChains = highRiskChains;
    }

    public List<String> getHighRiskTokens() {
      return highRiskTokens;
    }

    public void setHighRiskTokens(List<String> highRiskTokens) {
      this.highRiskTokens = highRiskTokens;
    }
  }

  public static class Scanner {

    private int confirmationDepth = 3;
    private int scanIntervalSeconds = 15;
    private int checkpointFlushBlocks = 20;
    private int backfillBlocks = 12;
    private int logScanBatchBlocks = 10;
    private int logScanRetryAttempts = 3;
    private int failureCooldownSeconds = 60;
    private int schedulerPoolSize = 4;
    private boolean websocketEnabled = true;
    private int websocketLeaderLeaseSeconds = 45;
    private long chainReplayTtlHours = 720;

    public int getConfirmationDepth() {
      return confirmationDepth;
    }

    public void setConfirmationDepth(int confirmationDepth) {
      this.confirmationDepth = confirmationDepth;
    }

    public int getScanIntervalSeconds() {
      return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(int scanIntervalSeconds) {
      this.scanIntervalSeconds = scanIntervalSeconds;
    }

    public int getCheckpointFlushBlocks() {
      return checkpointFlushBlocks;
    }

    public void setCheckpointFlushBlocks(int checkpointFlushBlocks) {
      this.checkpointFlushBlocks = checkpointFlushBlocks;
    }

    public int getBackfillBlocks() {
      return backfillBlocks;
    }

    public void setBackfillBlocks(int backfillBlocks) {
      this.backfillBlocks = backfillBlocks;
    }

    public int getLogScanBatchBlocks() {
      return logScanBatchBlocks;
    }

    public void setLogScanBatchBlocks(int logScanBatchBlocks) {
      this.logScanBatchBlocks = logScanBatchBlocks;
    }

    public int getLogScanRetryAttempts() {
      return logScanRetryAttempts;
    }

    public void setLogScanRetryAttempts(int logScanRetryAttempts) {
      this.logScanRetryAttempts = logScanRetryAttempts;
    }

    public int getFailureCooldownSeconds() {
      return failureCooldownSeconds;
    }

    public void setFailureCooldownSeconds(int failureCooldownSeconds) {
      this.failureCooldownSeconds = failureCooldownSeconds;
    }

    public int getSchedulerPoolSize() {
      return schedulerPoolSize;
    }

    public void setSchedulerPoolSize(int schedulerPoolSize) {
      this.schedulerPoolSize = schedulerPoolSize;
    }

    public boolean isWebsocketEnabled() {
      return websocketEnabled;
    }

    public void setWebsocketEnabled(boolean websocketEnabled) {
      this.websocketEnabled = websocketEnabled;
    }

    public int getWebsocketLeaderLeaseSeconds() {
      return websocketLeaderLeaseSeconds;
    }

    public void setWebsocketLeaderLeaseSeconds(int websocketLeaderLeaseSeconds) {
      this.websocketLeaderLeaseSeconds = websocketLeaderLeaseSeconds;
    }

    public long getChainReplayTtlHours() {
      return chainReplayTtlHours;
    }

    public void setChainReplayTtlHours(long chainReplayTtlHours) {
      this.chainReplayTtlHours = chainReplayTtlHours;
    }

    public static Scanner copyOf(Scanner source) {
      Scanner target = new Scanner();
      if (source == null) {
        return target;
      }
      target.setConfirmationDepth(source.getConfirmationDepth());
      target.setScanIntervalSeconds(source.getScanIntervalSeconds());
      target.setCheckpointFlushBlocks(source.getCheckpointFlushBlocks());
      target.setBackfillBlocks(source.getBackfillBlocks());
      target.setLogScanBatchBlocks(source.getLogScanBatchBlocks());
      target.setLogScanRetryAttempts(source.getLogScanRetryAttempts());
      target.setFailureCooldownSeconds(source.getFailureCooldownSeconds());
      target.setSchedulerPoolSize(source.getSchedulerPoolSize());
      target.setWebsocketEnabled(source.isWebsocketEnabled());
      target.setWebsocketLeaderLeaseSeconds(source.getWebsocketLeaderLeaseSeconds());
      target.setChainReplayTtlHours(source.getChainReplayTtlHours());
      return target;
    }
  }

  public static class Subscription {

    private boolean enabled = true;
    private String defaultMode = "SUPERFLUID_STREAM";
    private int defaultCycleSeconds = 2592000;
    private String superfluidHostAddress = "";
    private String superfluidCfaAddress = "";
    private String erc1337ExecutorAddress = "";
    private boolean schedulerEnabled = true;
    private int schedulerIntervalSeconds = 30;
    private int streamSettlementIntervalSeconds = 3600;
    private int schedulerBatchSize = 50;
    private int maxRetryCount = 3;
    private int retryBackoffSeconds = 300;
    private java.math.BigInteger executionGasLimit = new java.math.BigInteger("500000");
    private SecretSourceType executorPrivateKeySourceType = SecretSourceType.ENV;
    private String executorPrivateKey = "";
    private String executorPrivateKeyEnv = "CRYPTO_PAYMENT_SUBSCRIPTION_EXECUTOR_PRIVATE_KEY";
    private String executorPrivateKeyKmsKeyId = "subscription-executor-private-key";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getDefaultMode() {
      return defaultMode;
    }

    public void setDefaultMode(String defaultMode) {
      this.defaultMode = defaultMode;
    }

    public int getDefaultCycleSeconds() {
      return defaultCycleSeconds;
    }

    public void setDefaultCycleSeconds(int defaultCycleSeconds) {
      this.defaultCycleSeconds = defaultCycleSeconds;
    }

    public String getSuperfluidHostAddress() {
      return superfluidHostAddress;
    }

    public void setSuperfluidHostAddress(String superfluidHostAddress) {
      this.superfluidHostAddress = superfluidHostAddress;
    }

    public String getSuperfluidCfaAddress() {
      return superfluidCfaAddress;
    }

    public void setSuperfluidCfaAddress(String superfluidCfaAddress) {
      this.superfluidCfaAddress = superfluidCfaAddress;
    }

    public String getErc1337ExecutorAddress() {
      return erc1337ExecutorAddress;
    }

    public void setErc1337ExecutorAddress(String erc1337ExecutorAddress) {
      this.erc1337ExecutorAddress = erc1337ExecutorAddress;
    }

    public boolean isSchedulerEnabled() {
      return schedulerEnabled;
    }

    public void setSchedulerEnabled(boolean schedulerEnabled) {
      this.schedulerEnabled = schedulerEnabled;
    }

    public int getSchedulerIntervalSeconds() {
      return schedulerIntervalSeconds;
    }

    public void setSchedulerIntervalSeconds(int schedulerIntervalSeconds) {
      this.schedulerIntervalSeconds = schedulerIntervalSeconds;
    }

    public int getStreamSettlementIntervalSeconds() {
      return streamSettlementIntervalSeconds;
    }

    public void setStreamSettlementIntervalSeconds(int streamSettlementIntervalSeconds) {
      this.streamSettlementIntervalSeconds = streamSettlementIntervalSeconds;
    }

    public int getSchedulerBatchSize() {
      return schedulerBatchSize;
    }

    public void setSchedulerBatchSize(int schedulerBatchSize) {
      this.schedulerBatchSize = schedulerBatchSize;
    }

    public int getMaxRetryCount() {
      return maxRetryCount;
    }

    public void setMaxRetryCount(int maxRetryCount) {
      this.maxRetryCount = maxRetryCount;
    }

    public int getRetryBackoffSeconds() {
      return retryBackoffSeconds;
    }

    public void setRetryBackoffSeconds(int retryBackoffSeconds) {
      this.retryBackoffSeconds = retryBackoffSeconds;
    }

    public java.math.BigInteger getExecutionGasLimit() {
      return executionGasLimit;
    }

    public void setExecutionGasLimit(java.math.BigInteger executionGasLimit) {
      this.executionGasLimit = executionGasLimit;
    }

    public SecretSourceType getExecutorPrivateKeySourceType() {
      return executorPrivateKeySourceType;
    }

    public void setExecutorPrivateKeySourceType(SecretSourceType executorPrivateKeySourceType) {
      this.executorPrivateKeySourceType = executorPrivateKeySourceType;
    }

    public String getExecutorPrivateKey() {
      return executorPrivateKey;
    }

    public void setExecutorPrivateKey(String executorPrivateKey) {
      this.executorPrivateKey = executorPrivateKey;
    }

    public String getExecutorPrivateKeyEnv() {
      return executorPrivateKeyEnv;
    }

    public void setExecutorPrivateKeyEnv(String executorPrivateKeyEnv) {
      this.executorPrivateKeyEnv = executorPrivateKeyEnv;
    }

    public String getExecutorPrivateKeyKmsKeyId() {
      return executorPrivateKeyKmsKeyId;
    }

    public void setExecutorPrivateKeyKmsKeyId(String executorPrivateKeyKmsKeyId) {
      this.executorPrivateKeyKmsKeyId = executorPrivateKeyKmsKeyId;
    }
  }

  public static class Gas {

    private boolean enabled = true;
    private boolean hostedWalletPreferPlatform = true;
    private boolean hostedWalletSponsorEnabled = true;
    private boolean evmSponsorEnabled = false;
    private SecretSourceType evmRelayerPrivateKeySourceType = SecretSourceType.ENV;
    private String evmRelayerPrivateKey = "";
    private String evmRelayerPrivateKeyEnv = "CRYPTO_PAYMENT_EVM_RELAYER_PRIVATE_KEY";
    private String evmRelayerPrivateKeyKmsKeyId = "evm-relayer-private-key";
    private boolean sponsorProtectionEnabled = true;
    private int sponsorCounterTtlSeconds = 3600;
    private int sponsorOrderMaxAttempts = 3;
    private int sponsorWalletMaxAttempts = 10;
    private int sponsorIpMaxAttempts = 30;
    private int sponsorCashierTokenMaxAttempts = 10;
    private int sponsorOrderLockLeaseSeconds = 60;
    private boolean customerBalancePrecheckEnabled = true;
    private int platformSponsoredGasLimit = 250000;
    private int customerGasLimit = 120000;
    private int freeTransferGasLimit = 21000;
    private List<String> lowFeeChainCandidates = new ArrayList<>();

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isHostedWalletPreferPlatform() {
      return hostedWalletPreferPlatform;
    }

    public void setHostedWalletPreferPlatform(boolean hostedWalletPreferPlatform) {
      this.hostedWalletPreferPlatform = hostedWalletPreferPlatform;
    }

    public boolean isHostedWalletSponsorEnabled() {
      return hostedWalletSponsorEnabled;
    }

    public void setHostedWalletSponsorEnabled(boolean hostedWalletSponsorEnabled) {
      this.hostedWalletSponsorEnabled = hostedWalletSponsorEnabled;
    }

    public boolean isEvmSponsorEnabled() {
      return evmSponsorEnabled;
    }

    public void setEvmSponsorEnabled(boolean evmSponsorEnabled) {
      this.evmSponsorEnabled = evmSponsorEnabled;
    }

    public SecretSourceType getEvmRelayerPrivateKeySourceType() {
      return evmRelayerPrivateKeySourceType;
    }

    public void setEvmRelayerPrivateKeySourceType(SecretSourceType evmRelayerPrivateKeySourceType) {
      this.evmRelayerPrivateKeySourceType = evmRelayerPrivateKeySourceType;
    }

    public String getEvmRelayerPrivateKey() {
      return evmRelayerPrivateKey;
    }

    public void setEvmRelayerPrivateKey(String evmRelayerPrivateKey) {
      this.evmRelayerPrivateKey = evmRelayerPrivateKey;
    }

    public String getEvmRelayerPrivateKeyEnv() {
      return evmRelayerPrivateKeyEnv;
    }

    public void setEvmRelayerPrivateKeyEnv(String evmRelayerPrivateKeyEnv) {
      this.evmRelayerPrivateKeyEnv = evmRelayerPrivateKeyEnv;
    }

    public String getEvmRelayerPrivateKeyKmsKeyId() {
      return evmRelayerPrivateKeyKmsKeyId;
    }

    public void setEvmRelayerPrivateKeyKmsKeyId(String evmRelayerPrivateKeyKmsKeyId) {
      this.evmRelayerPrivateKeyKmsKeyId = evmRelayerPrivateKeyKmsKeyId;
    }

    public boolean isSponsorProtectionEnabled() {
      return sponsorProtectionEnabled;
    }

    public void setSponsorProtectionEnabled(boolean sponsorProtectionEnabled) {
      this.sponsorProtectionEnabled = sponsorProtectionEnabled;
    }

    public int getSponsorCounterTtlSeconds() {
      return sponsorCounterTtlSeconds;
    }

    public void setSponsorCounterTtlSeconds(int sponsorCounterTtlSeconds) {
      this.sponsorCounterTtlSeconds = sponsorCounterTtlSeconds;
    }

    public int getSponsorOrderMaxAttempts() {
      return sponsorOrderMaxAttempts;
    }

    public void setSponsorOrderMaxAttempts(int sponsorOrderMaxAttempts) {
      this.sponsorOrderMaxAttempts = sponsorOrderMaxAttempts;
    }

    public int getSponsorWalletMaxAttempts() {
      return sponsorWalletMaxAttempts;
    }

    public void setSponsorWalletMaxAttempts(int sponsorWalletMaxAttempts) {
      this.sponsorWalletMaxAttempts = sponsorWalletMaxAttempts;
    }

    public int getSponsorIpMaxAttempts() {
      return sponsorIpMaxAttempts;
    }

    public void setSponsorIpMaxAttempts(int sponsorIpMaxAttempts) {
      this.sponsorIpMaxAttempts = sponsorIpMaxAttempts;
    }

    public int getSponsorCashierTokenMaxAttempts() {
      return sponsorCashierTokenMaxAttempts;
    }

    public void setSponsorCashierTokenMaxAttempts(int sponsorCashierTokenMaxAttempts) {
      this.sponsorCashierTokenMaxAttempts = sponsorCashierTokenMaxAttempts;
    }

    public int getSponsorOrderLockLeaseSeconds() {
      return sponsorOrderLockLeaseSeconds;
    }

    public void setSponsorOrderLockLeaseSeconds(int sponsorOrderLockLeaseSeconds) {
      this.sponsorOrderLockLeaseSeconds = sponsorOrderLockLeaseSeconds;
    }

    public boolean isCustomerBalancePrecheckEnabled() {
      return customerBalancePrecheckEnabled;
    }

    public void setCustomerBalancePrecheckEnabled(boolean customerBalancePrecheckEnabled) {
      this.customerBalancePrecheckEnabled = customerBalancePrecheckEnabled;
    }

    public int getPlatformSponsoredGasLimit() {
      return platformSponsoredGasLimit;
    }

    public void setPlatformSponsoredGasLimit(int platformSponsoredGasLimit) {
      this.platformSponsoredGasLimit = platformSponsoredGasLimit;
    }

    public int getCustomerGasLimit() {
      return customerGasLimit;
    }

    public void setCustomerGasLimit(int customerGasLimit) {
      this.customerGasLimit = customerGasLimit;
    }

    public int getFreeTransferGasLimit() {
      return freeTransferGasLimit;
    }

    public void setFreeTransferGasLimit(int freeTransferGasLimit) {
      this.freeTransferGasLimit = freeTransferGasLimit;
    }

    public List<String> getLowFeeChainCandidates() {
      return lowFeeChainCandidates;
    }

    public void setLowFeeChainCandidates(List<String> lowFeeChainCandidates) {
      this.lowFeeChainCandidates = lowFeeChainCandidates;
    }
  }

  public static class Solana {

    private boolean feePayerEnabled = false;
    private String feePayerAddress = "";
    private SecretSourceType feePayerPrivateKeySourceType = SecretSourceType.ENV;
    private String feePayerPrivateKey = "";
    private String feePayerPrivateKeyEnv = "CRYPTO_PAYMENT_SOLANA_FEE_PAYER_PRIVATE_KEY";
    private String feePayerPrivateKeyKmsKeyId = "solana-fee-payer-private-key";
    private long defaultFeeLamportsPerSignature = 5000L;
    private boolean heliusWebhookEnabled = false;
    private String heliusWebhookSecret = "";
    private String heliusWebhookSecretHeader = "X-Helius-Webhook-Secret";

    public boolean isFeePayerEnabled() {
      return feePayerEnabled;
    }

    public void setFeePayerEnabled(boolean feePayerEnabled) {
      this.feePayerEnabled = feePayerEnabled;
    }

    public String getFeePayerAddress() {
      return feePayerAddress;
    }

    public void setFeePayerAddress(String feePayerAddress) {
      this.feePayerAddress = feePayerAddress;
    }

    public SecretSourceType getFeePayerPrivateKeySourceType() {
      return feePayerPrivateKeySourceType;
    }

    public void setFeePayerPrivateKeySourceType(SecretSourceType feePayerPrivateKeySourceType) {
      this.feePayerPrivateKeySourceType = feePayerPrivateKeySourceType;
    }

    public String getFeePayerPrivateKey() {
      return feePayerPrivateKey;
    }

    public void setFeePayerPrivateKey(String feePayerPrivateKey) {
      this.feePayerPrivateKey = feePayerPrivateKey;
    }

    public String getFeePayerPrivateKeyEnv() {
      return feePayerPrivateKeyEnv;
    }

    public void setFeePayerPrivateKeyEnv(String feePayerPrivateKeyEnv) {
      this.feePayerPrivateKeyEnv = feePayerPrivateKeyEnv;
    }

    public String getFeePayerPrivateKeyKmsKeyId() {
      return feePayerPrivateKeyKmsKeyId;
    }

    public void setFeePayerPrivateKeyKmsKeyId(String feePayerPrivateKeyKmsKeyId) {
      this.feePayerPrivateKeyKmsKeyId = feePayerPrivateKeyKmsKeyId;
    }

    public long getDefaultFeeLamportsPerSignature() {
      return defaultFeeLamportsPerSignature;
    }

    public void setDefaultFeeLamportsPerSignature(long defaultFeeLamportsPerSignature) {
      this.defaultFeeLamportsPerSignature = defaultFeeLamportsPerSignature;
    }

    public boolean isHeliusWebhookEnabled() {
      return heliusWebhookEnabled;
    }

    public void setHeliusWebhookEnabled(boolean heliusWebhookEnabled) {
      this.heliusWebhookEnabled = heliusWebhookEnabled;
    }

    public String getHeliusWebhookSecret() {
      return heliusWebhookSecret;
    }

    public void setHeliusWebhookSecret(String heliusWebhookSecret) {
      this.heliusWebhookSecret = heliusWebhookSecret;
    }

    public String getHeliusWebhookSecretHeader() {
      return heliusWebhookSecretHeader;
    }

    public void setHeliusWebhookSecretHeader(String heliusWebhookSecretHeader) {
      this.heliusWebhookSecretHeader = heliusWebhookSecretHeader;
    }
  }

  public static class Redis {

    private boolean enabled = false;
    private String url = "redis://127.0.0.1:6379";
    private String password;
    private int database = 0;
    private String keyPrefix = "crypto:payment";
    private boolean preloadEnabled = true;
    private int leaseMinutes = 30;
    private int lockWaitMillis = 2000;
    private int lockLeaseSeconds = 8;
    private boolean autoCreateEnabled = true;
    private int autoCreateBatchSize = 1;
    private int refillThreshold = 2;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public int getDatabase() {
      return database;
    }

    public void setDatabase(int database) {
      this.database = database;
    }

    public String getKeyPrefix() {
      return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    public boolean isPreloadEnabled() {
      return preloadEnabled;
    }

    public void setPreloadEnabled(boolean preloadEnabled) {
      this.preloadEnabled = preloadEnabled;
    }

    public int getLeaseMinutes() {
      return leaseMinutes;
    }

    public void setLeaseMinutes(int leaseMinutes) {
      this.leaseMinutes = leaseMinutes;
    }

    public int getLockWaitMillis() {
      return lockWaitMillis;
    }

    public void setLockWaitMillis(int lockWaitMillis) {
      this.lockWaitMillis = lockWaitMillis;
    }

    public int getLockLeaseSeconds() {
      return lockLeaseSeconds;
    }

    public void setLockLeaseSeconds(int lockLeaseSeconds) {
      this.lockLeaseSeconds = lockLeaseSeconds;
    }

    public boolean isAutoCreateEnabled() {
      return autoCreateEnabled;
    }

    public void setAutoCreateEnabled(boolean autoCreateEnabled) {
      this.autoCreateEnabled = autoCreateEnabled;
    }

    public int getAutoCreateBatchSize() {
      return autoCreateBatchSize;
    }

    public void setAutoCreateBatchSize(int autoCreateBatchSize) {
      this.autoCreateBatchSize = autoCreateBatchSize;
    }

    public int getRefillThreshold() {
      return refillThreshold;
    }

    public void setRefillThreshold(int refillThreshold) {
      this.refillThreshold = refillThreshold;
    }
  }

  public static class Signature {

    private boolean enabled = true;
    private boolean verifyInboundEnabled = true;
    private boolean signOutboundEnabled = true;
    private String algorithm = "RSA_SHA256";
    private String merchantIdHeader = "X-Merchant-Id";
    private String timestampHeader = "X-Timestamp";
    private String nonceHeader = "X-Nonce";
    private String signatureHeader = "X-Signature";
    private String keyVersionHeader = "X-Key-Version";
    private long allowedClockSkewSeconds = 300;
    private long replayTtlSeconds = 600;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isVerifyInboundEnabled() {
      return verifyInboundEnabled;
    }

    public void setVerifyInboundEnabled(boolean verifyInboundEnabled) {
      this.verifyInboundEnabled = verifyInboundEnabled;
    }

    public boolean isSignOutboundEnabled() {
      return signOutboundEnabled;
    }

    public void setSignOutboundEnabled(boolean signOutboundEnabled) {
      this.signOutboundEnabled = signOutboundEnabled;
    }

    public String getAlgorithm() {
      return algorithm;
    }

    public void setAlgorithm(String algorithm) {
      this.algorithm = algorithm;
    }

    public String getMerchantIdHeader() {
      return merchantIdHeader;
    }

    public void setMerchantIdHeader(String merchantIdHeader) {
      this.merchantIdHeader = merchantIdHeader;
    }

    public String getTimestampHeader() {
      return timestampHeader;
    }

    public void setTimestampHeader(String timestampHeader) {
      this.timestampHeader = timestampHeader;
    }

    public String getNonceHeader() {
      return nonceHeader;
    }

    public void setNonceHeader(String nonceHeader) {
      this.nonceHeader = nonceHeader;
    }

    public String getSignatureHeader() {
      return signatureHeader;
    }

    public void setSignatureHeader(String signatureHeader) {
      this.signatureHeader = signatureHeader;
    }

    public String getKeyVersionHeader() {
      return keyVersionHeader;
    }

    public void setKeyVersionHeader(String keyVersionHeader) {
      this.keyVersionHeader = keyVersionHeader;
    }

    public long getAllowedClockSkewSeconds() {
      return allowedClockSkewSeconds;
    }

    public void setAllowedClockSkewSeconds(long allowedClockSkewSeconds) {
      this.allowedClockSkewSeconds = allowedClockSkewSeconds;
    }

    public long getReplayTtlSeconds() {
      return replayTtlSeconds;
    }

    public void setReplayTtlSeconds(long replayTtlSeconds) {
      this.replayTtlSeconds = replayTtlSeconds;
    }
  }

  public static class Callback {

    private boolean retryEnabled = true;
    private int maxAttempts = 6;
    private long initialBackoffSeconds = 15;
    private long maxBackoffSeconds = 900;
    private long retentionDays = 14;
    private boolean deadLetterEnabled = true;
    private boolean dispatchImmediately = true;
    private int dispatchBatchSize = 100;
    private long dispatchLockWaitMillis = 2000;
    private long dispatchLockLeaseSeconds = 60;
    private int maxStoredResponseChars = 8000;
    private int maxStoredErrorChars = 4000;

    public boolean isRetryEnabled() {
      return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
      this.retryEnabled = retryEnabled;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public long getInitialBackoffSeconds() {
      return initialBackoffSeconds;
    }

    public void setInitialBackoffSeconds(long initialBackoffSeconds) {
      this.initialBackoffSeconds = initialBackoffSeconds;
    }

    public long getMaxBackoffSeconds() {
      return maxBackoffSeconds;
    }

    public void setMaxBackoffSeconds(long maxBackoffSeconds) {
      this.maxBackoffSeconds = maxBackoffSeconds;
    }

    public long getRetentionDays() {
      return retentionDays;
    }

    public void setRetentionDays(long retentionDays) {
      this.retentionDays = retentionDays;
    }

    public boolean isDeadLetterEnabled() {
      return deadLetterEnabled;
    }

    public void setDeadLetterEnabled(boolean deadLetterEnabled) {
      this.deadLetterEnabled = deadLetterEnabled;
    }

    public boolean isDispatchImmediately() {
      return dispatchImmediately;
    }

    public void setDispatchImmediately(boolean dispatchImmediately) {
      this.dispatchImmediately = dispatchImmediately;
    }

    public int getDispatchBatchSize() {
      return dispatchBatchSize;
    }

    public void setDispatchBatchSize(int dispatchBatchSize) {
      this.dispatchBatchSize = dispatchBatchSize;
    }

    public long getDispatchLockWaitMillis() {
      return dispatchLockWaitMillis;
    }

    public void setDispatchLockWaitMillis(long dispatchLockWaitMillis) {
      this.dispatchLockWaitMillis = dispatchLockWaitMillis;
    }

    public long getDispatchLockLeaseSeconds() {
      return dispatchLockLeaseSeconds;
    }

    public void setDispatchLockLeaseSeconds(long dispatchLockLeaseSeconds) {
      this.dispatchLockLeaseSeconds = dispatchLockLeaseSeconds;
    }

    public int getMaxStoredResponseChars() {
      return maxStoredResponseChars;
    }

    public void setMaxStoredResponseChars(int maxStoredResponseChars) {
      this.maxStoredResponseChars = maxStoredResponseChars;
    }

    public int getMaxStoredErrorChars() {
      return maxStoredErrorChars;
    }

    public void setMaxStoredErrorChars(int maxStoredErrorChars) {
      this.maxStoredErrorChars = maxStoredErrorChars;
    }
  }

  public static class Security {

    private boolean validateRedirectUrl = true;
    private boolean allowLocalRedirect = false;
    private List<String> allowedRedirectHosts = new ArrayList<>();

    public boolean isValidateRedirectUrl() {
      return validateRedirectUrl;
    }

    public void setValidateRedirectUrl(boolean validateRedirectUrl) {
      this.validateRedirectUrl = validateRedirectUrl;
    }

    public boolean isAllowLocalRedirect() {
      return allowLocalRedirect;
    }

    public void setAllowLocalRedirect(boolean allowLocalRedirect) {
      this.allowLocalRedirect = allowLocalRedirect;
    }

    public List<String> getAllowedRedirectHosts() {
      return allowedRedirectHosts;
    }

    public void setAllowedRedirectHosts(List<String> allowedRedirectHosts) {
      this.allowedRedirectHosts = allowedRedirectHosts;
    }
  }

  public static class Cashier {

    private boolean tokenEncryptionEnabled = true;
    private String tokenKeyAlias = "cashier-v1";
    private int tokenTtlMinutes = 60;
    private String tokenPrefix = "cashier";
    private boolean shortTokenEnabled = true;
    private String shortTokenPrefix = "c";
    private int shortTokenLength = 12;

    public boolean isTokenEncryptionEnabled() {
      return tokenEncryptionEnabled;
    }

    public void setTokenEncryptionEnabled(boolean tokenEncryptionEnabled) {
      this.tokenEncryptionEnabled = tokenEncryptionEnabled;
    }

    public String getTokenKeyAlias() {
      return tokenKeyAlias;
    }

    public void setTokenKeyAlias(String tokenKeyAlias) {
      this.tokenKeyAlias = tokenKeyAlias;
    }

    public int getTokenTtlMinutes() {
      return tokenTtlMinutes;
    }

    public void setTokenTtlMinutes(int tokenTtlMinutes) {
      this.tokenTtlMinutes = tokenTtlMinutes;
    }

    public String getTokenPrefix() {
      return tokenPrefix;
    }

    public void setTokenPrefix(String tokenPrefix) {
      this.tokenPrefix = tokenPrefix;
    }

    public boolean isShortTokenEnabled() {
      return shortTokenEnabled;
    }

    public void setShortTokenEnabled(boolean shortTokenEnabled) {
      this.shortTokenEnabled = shortTokenEnabled;
    }

    public String getShortTokenPrefix() {
      return shortTokenPrefix;
    }

    public void setShortTokenPrefix(String shortTokenPrefix) {
      this.shortTokenPrefix = shortTokenPrefix;
    }

    public int getShortTokenLength() {
      return shortTokenLength;
    }

    public void setShortTokenLength(int shortTokenLength) {
      this.shortTokenLength = shortTokenLength;
    }
  }

  public static class Secrets {

    private Kms kms = new Kms();
    private Jni jni = new Jni();

    public Kms getKms() {
      return kms;
    }

    public void setKms(Kms kms) {
      this.kms = kms;
    }

    public Jni getJni() {
      return jni;
    }

    public void setJni(Jni jni) {
      this.jni = jni;
    }
  }

  public static class Kms {

    private boolean enabled = false;
    private String baseUrl = "";
    private String resolvePath = "/secrets/{keyId}";
    private String responseField = "value";
    private String apiKey;
    private String apiKeyEnv = "CRYPTO_PAYMENT_KMS_API_KEY";
    private String apiKeyHeader = "Authorization";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getResolvePath() {
      return resolvePath;
    }

    public void setResolvePath(String resolvePath) {
      this.resolvePath = resolvePath;
    }

    public String getResponseField() {
      return responseField;
    }

    public void setResponseField(String responseField) {
      this.responseField = responseField;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getApiKeyEnv() {
      return apiKeyEnv;
    }

    public void setApiKeyEnv(String apiKeyEnv) {
      this.apiKeyEnv = apiKeyEnv;
    }

    public String getApiKeyHeader() {
      return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
      this.apiKeyHeader = apiKeyHeader;
    }
  }

  public static class Jni {

    private boolean enabled = false;
    private String libraryPath = "";
    private String libraryName = "crypto_secret_jni";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getLibraryPath() {
      return libraryPath;
    }

    public void setLibraryPath(String libraryPath) {
      this.libraryPath = libraryPath;
    }

    public String getLibraryName() {
      return libraryName;
    }

    public void setLibraryName(String libraryName) {
      this.libraryName = libraryName;
    }
  }

  public static class Messaging {

    private String provider = "REDIS_STREAM";
    private boolean consumerEnabled = true;
    private String chainPaymentTopic = "crypto:payment:chain-payment-events";
    private String subscriptionEventTopic = "crypto:payment:subscription-events";
    private String streamConsumerGroup = "crypto-payment";
    private String streamConsumerName = "";
    private int streamPollMillis = 1000;
    private int streamBatchSize = 20;
    private int streamPendingClaimIdleSeconds = 60;

    public String getProvider() {
      return provider;
    }

    public void setProvider(String provider) {
      this.provider = provider;
    }

    public boolean isConsumerEnabled() {
      return consumerEnabled;
    }

    public void setConsumerEnabled(boolean consumerEnabled) {
      this.consumerEnabled = consumerEnabled;
    }

    public String getChainPaymentTopic() {
      return chainPaymentTopic;
    }

    public void setChainPaymentTopic(String chainPaymentTopic) {
      this.chainPaymentTopic = chainPaymentTopic;
    }

    public String getSubscriptionEventTopic() {
      return subscriptionEventTopic;
    }

    public void setSubscriptionEventTopic(String subscriptionEventTopic) {
      this.subscriptionEventTopic = subscriptionEventTopic;
    }

    public String getStreamConsumerGroup() {
      return streamConsumerGroup;
    }

    public void setStreamConsumerGroup(String streamConsumerGroup) {
      this.streamConsumerGroup = streamConsumerGroup;
    }

    public String getStreamConsumerName() {
      return streamConsumerName;
    }

    public void setStreamConsumerName(String streamConsumerName) {
      this.streamConsumerName = streamConsumerName;
    }

    public int getStreamPollMillis() {
      return streamPollMillis;
    }

    public void setStreamPollMillis(int streamPollMillis) {
      this.streamPollMillis = streamPollMillis;
    }

    public int getStreamBatchSize() {
      return streamBatchSize;
    }

    public void setStreamBatchSize(int streamBatchSize) {
      this.streamBatchSize = streamBatchSize;
    }

    public int getStreamPendingClaimIdleSeconds() {
      return streamPendingClaimIdleSeconds;
    }

    public void setStreamPendingClaimIdleSeconds(int streamPendingClaimIdleSeconds) {
      this.streamPendingClaimIdleSeconds = streamPendingClaimIdleSeconds;
    }
  }

  public static class ExternalHttp {

    private int maxIdleConnections = 64;
    private int keepAliveSeconds = 300;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 8000;
    private int writeTimeoutMillis = 8000;
    private int callTimeoutMillis = 10000;
    private boolean retryOnConnectionFailure = true;

    public int getMaxIdleConnections() {
      return maxIdleConnections;
    }

    public void setMaxIdleConnections(int maxIdleConnections) {
      this.maxIdleConnections = maxIdleConnections;
    }

    public int getKeepAliveSeconds() {
      return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
      this.keepAliveSeconds = keepAliveSeconds;
    }

    public int getConnectTimeoutMillis() {
      return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
      this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
      return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
    }

    public int getWriteTimeoutMillis() {
      return writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
      this.writeTimeoutMillis = writeTimeoutMillis;
    }

    public int getCallTimeoutMillis() {
      return callTimeoutMillis;
    }

    public void setCallTimeoutMillis(int callTimeoutMillis) {
      this.callTimeoutMillis = callTimeoutMillis;
    }

    public boolean isRetryOnConnectionFailure() {
      return retryOnConnectionFailure;
    }

    public void setRetryOnConnectionFailure(boolean retryOnConnectionFailure) {
      this.retryOnConnectionFailure = retryOnConnectionFailure;
    }
  }
}
