package io.swzxsyh.payment.gas;

import io.swzxsyh.payment.chain.ChainClient;
import io.swzxsyh.payment.chain.ChainClientFactory;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.chain.SolanaChainClient;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.routing.TokenRouteType;
import io.swzxsyh.payment.routing.WalletAccountType;
import io.swzxsyh.payment.sponsor.GasSponsorshipPolicyService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class LocalGasPolicyProvider implements GasPolicyProvider {

  private static final BigDecimal WEI_IN_GWEI = new BigDecimal("1000000000");

  private final CryptoPaymentProperties properties;
  private final ChainClientFactory chainClientFactory;
  private final GasSponsorshipPolicyService sponsorshipPolicyService;

  public LocalGasPolicyProvider(
      CryptoPaymentProperties properties,
      ChainClientFactory chainClientFactory,
      GasSponsorshipPolicyService sponsorshipPolicyService) {
    this.properties = properties;
    this.chainClientFactory = chainClientFactory;
    this.sponsorshipPolicyService = sponsorshipPolicyService;
  }

  @Override
  public String providerId() {
    return "local-gas-policy";
  }

  @Override
  public GasPolicyResult plan(GasPolicyRequest request) {
    if (!properties.getGas().isEnabled()) {
      return buildDisabledResult();
    }
    ChainFamily family = ChainFamilyResolver.resolve(request.chain());
    if (family == ChainFamily.SOLANA) {
      return buildSolanaResult(request);
    }
    if (family != ChainFamily.EVM) {
      return buildNonEvmResult(request);
    }
    if (request.paymentMethod() == PaymentMethod.DERIVED_ADDRESS) {
      return buildFreeTransferResult(request);
    }

    ChainClient chainClient = chainClientFactory.get(request.chain());
    BigDecimal gasPriceWei = safeGasPrice(chainClient);
    boolean walletKnown = StringUtils.hasText(request.walletAddress());
    BigDecimal walletBalanceWei = walletKnown ? safeBalance(chainClient, request.walletAddress()) : BigDecimal.ZERO;
    BigDecimal treasuryBalanceWei = safeBalance(chainClient, properties.getTreasuryAddress());

    BigDecimal gasLimit = resolveGasLimit(request);
    BigDecimal estimatedFeeWei = gasLimit.multiply(gasPriceWei);
    boolean customerSufficient = walletBalanceWei.compareTo(estimatedFeeWei) >= 0;
    boolean platformSufficient = treasuryBalanceWei.compareTo(estimatedFeeWei) >= 0;
    boolean sponsorEnabled = isEvmSponsorEnabled(request);

    List<String> reasons = new ArrayList<>();
    GasPayerMode payerMode;
    String fallbackSuggestion = null;

    if (request.walletAccountType() == WalletAccountType.SCA) {
      if (properties.getGas().isHostedWalletPreferPlatform() && sponsorEnabled) {
        if (platformSufficient) {
          payerMode = GasPayerMode.PLATFORM_SPONSORED;
          reasons.add("hosted wallet prefers platform sponsored gas");
        } else if (customerSufficient) {
          payerMode = GasPayerMode.CUSTOMER_PAYS;
          reasons.add("platform treasury balance not enough, fallback to customer gas payment");
          fallbackSuggestion = buildFallbackSuggestion(request);
        } else {
          payerMode = GasPayerMode.CUSTOMER_TOP_UP_REQUIRED;
          reasons.add("platform treasury and customer wallet are both below estimated gas");
          fallbackSuggestion = buildFallbackSuggestion(request);
        }
      } else if (customerSufficient) {
        payerMode = GasPayerMode.CUSTOMER_PAYS;
        reasons.add("hosted wallet does not use sponsor path, customer pays gas");
      } else {
        payerMode = GasPayerMode.CUSTOMER_TOP_UP_REQUIRED;
        reasons.add("hosted wallet customer balance is not enough for gas");
        fallbackSuggestion = buildFallbackSuggestion(request);
      }
    } else {
      if (!walletKnown) {
        payerMode = request.tokenRouteType() == TokenRouteType.SMART_CONTRACT_SETTLEMENT
                || request.tokenRouteType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION && sponsorEnabled
            ? GasPayerMode.PLATFORM_SPONSORED
            : GasPayerMode.CUSTOMER_PAYS;
        reasons.add("customer wallet address is unknown, skip customer gas balance precheck");
      } else if (customerSufficient) {
        payerMode = request.tokenRouteType() == TokenRouteType.SMART_CONTRACT_SETTLEMENT
                || request.tokenRouteType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION && sponsorEnabled
            ? GasPayerMode.PLATFORM_SPONSORED
            : GasPayerMode.CUSTOMER_PAYS;
        reasons.add("customer wallet can afford estimated gas");
      } else {
        payerMode = GasPayerMode.CUSTOMER_TOP_UP_REQUIRED;
        reasons.add("customer wallet cannot afford estimated gas");
        fallbackSuggestion = buildFallbackSuggestion(request);
      }
    }

    return new GasPolicyResult(
        true,
        payerMode,
        providerId(),
        platformSufficient,
        customerSufficient,
        gasLimit,
        gasPriceWei,
        estimatedFeeWei,
        walletBalanceWei,
        treasuryBalanceWei,
        String.join("; ", reasons),
        fallbackSuggestion,
        reasons,
        LocalDateTime.now()
    );
  }

  private GasPolicyResult buildFreeTransferResult(GasPolicyRequest request) {
    BigDecimal gasLimit = BigDecimal.valueOf(properties.getGas().getFreeTransferGasLimit());
    return new GasPolicyResult(
        true,
        GasPayerMode.FREE_TRANSFER,
        providerId(),
        true,
        true,
        gasLimit,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        "derived address transfer does not require platform-side gas precheck",
        null,
        List.of("free transfer route"),
        LocalDateTime.now()
    );
  }

  private GasPolicyResult buildDisabledResult() {
    return new GasPolicyResult(
        false,
        GasPayerMode.CUSTOMER_PAYS,
        providerId(),
        true,
        true,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        "gas policy disabled",
        null,
        List.of("gas disabled"),
        LocalDateTime.now()
    );
  }

  private GasPolicyResult buildNonEvmResult(GasPolicyRequest request) {
    return new GasPolicyResult(
        false,
        request.paymentMethod() == PaymentMethod.DERIVED_ADDRESS
            ? GasPayerMode.FREE_TRANSFER
            : GasPayerMode.CUSTOMER_PAYS,
        providerId(),
        true,
        true,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        "non-EVM chain skips EVM gas precheck",
        null,
        List.of("non-EVM chain"),
        LocalDateTime.now()
    );
  }

  private GasPolicyResult buildSolanaResult(GasPolicyRequest request) {
    boolean walletKnown = StringUtils.hasText(request.walletAddress());
    BigDecimal estimatedLamports =
        BigDecimal.valueOf(Math.max(1L, properties.getSolana().getDefaultFeeLamportsPerSignature()));
    BigDecimal walletBalanceLamports = BigDecimal.ZERO;
    BigDecimal feePayerBalanceLamports = BigDecimal.ZERO;
    String feePayerAddress = properties.getSolana().getFeePayerAddress();
    try {
      ChainClient chainClient = chainClientFactory.get(request.chain());
      if (chainClient instanceof SolanaChainClient solanaClient) {
        solanaClient.getLatestBlockhash();
        if (walletKnown) {
          walletBalanceLamports = safeBalance(chainClient, request.walletAddress());
        }
        if (StringUtils.hasText(feePayerAddress)) {
          feePayerBalanceLamports = safeBalance(chainClient, feePayerAddress);
        }
      }
    } catch (Exception ex) {
      log.warn("Failed to run Solana fee precheck, fallback to static lamports. chain={}, error={}",
          request.chain(), ex.getMessage());
    }

    boolean customerSufficient = !walletKnown || walletBalanceLamports.compareTo(estimatedLamports) >= 0;
    boolean platformSufficient =
        !sponsorshipPolicyService.isSponsorEnabled(request.chain(), "SOLANA_FEE_PAYER")
            || StringUtils.hasText(feePayerAddress) && feePayerBalanceLamports.compareTo(estimatedLamports) >= 0;
    GasPayerMode payerMode;
    List<String> reasons = new ArrayList<>();
    if (request.paymentMethod() == PaymentMethod.DERIVED_ADDRESS) {
      payerMode = GasPayerMode.FREE_TRANSFER;
      reasons.add("Solana free transfer route; customer wallet pays network fee outside platform");
    } else if (sponsorshipPolicyService.isSponsorEnabled(request.chain(), "SOLANA_FEE_PAYER") && platformSufficient) {
      payerMode = GasPayerMode.PLATFORM_SPONSORED;
      reasons.add("Solana fee payer is enabled; platform fee payer can sponsor lamports fee");
    } else if (customerSufficient) {
      payerMode = GasPayerMode.CUSTOMER_PAYS;
      reasons.add("Solana customer wallet can pay estimated lamports fee");
    } else {
      payerMode = GasPayerMode.CUSTOMER_TOP_UP_REQUIRED;
      reasons.add("Solana customer wallet balance is below estimated lamports fee");
    }
    return new GasPolicyResult(
        true,
        payerMode,
        providerId(),
        platformSufficient,
        customerSufficient,
        BigDecimal.ONE,
        estimatedLamports,
        estimatedLamports,
        walletBalanceLamports,
        feePayerBalanceLamports,
        "Solana fee precheck uses lamports, not EVM wei: " + String.join("; ", reasons),
        payerMode == GasPayerMode.CUSTOMER_TOP_UP_REQUIRED ? buildFallbackSuggestion(request) : null,
        reasons,
        LocalDateTime.now());
  }

  private BigDecimal resolveGasLimit(GasPolicyRequest request) {
    if (request.paymentMethod() == PaymentMethod.DERIVED_ADDRESS) {
      return BigDecimal.valueOf(properties.getGas().getFreeTransferGasLimit());
    }
    if (request.tokenRouteType() == TokenRouteType.SMART_CONTRACT_SETTLEMENT) {
      return BigDecimal.valueOf(properties.getGas().getPlatformSponsoredGasLimit());
    }
    if (request.tokenRouteType() == TokenRouteType.USER_APPROVE) {
      return BigDecimal.valueOf(properties.getGas().getCustomerGasLimit());
    }
    if (request.tokenRouteType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION) {
      return BigDecimal.valueOf(properties.getGas().getCustomerGasLimit() - 15000L);
    }
    if (request.tokenRouteType() == TokenRouteType.PERMIT_SIGNATURE) {
      return BigDecimal.valueOf(properties.getGas().getCustomerGasLimit() - 30000L);
    }
    return BigDecimal.valueOf(properties.getGas().getCustomerGasLimit());
  }

  private boolean isEvmSponsorEnabled(GasPolicyRequest request) {
    String providerId = request.tokenRouteType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION
        ? "EIP3009"
        : "AUTO";
    return sponsorshipPolicyService.isSponsorEnabled(request.chain(), providerId);
  }

  private BigDecimal safeGasPrice(ChainClient chainClient) {
    try {
      BigDecimal gasPriceWei = chainClient.getGasPrice();
      if (gasPriceWei == null || gasPriceWei.signum() <= 0) {
        return WEI_IN_GWEI.multiply(BigDecimal.ONE);
      }
      return gasPriceWei;
    } catch (Exception ex) {
      log.warn("Gas price query failed, fallback to 1 gwei. error={}", ex.getMessage());
      return WEI_IN_GWEI.multiply(BigDecimal.ONE);
    }
  }

  private BigDecimal safeBalance(ChainClient chainClient, String address) {
    if (!StringUtils.hasText(address)) {
      return BigDecimal.ZERO;
    }
    try {
      BigDecimal balanceWei = chainClient.getNativeBalance(address);
      return balanceWei == null ? BigDecimal.ZERO : balanceWei;
    } catch (Exception ex) {
      log.warn("Native balance query failed, fallback to zero. address={}, error={}", address, ex.getMessage());
      return BigDecimal.ZERO;
    }
  }

  private String buildFallbackSuggestion(GasPolicyRequest request) {
    List<String> candidates = properties.getGas().getLowFeeChainCandidates();
    if (candidates != null && !candidates.isEmpty()) {
      return "switch_chain:" + candidates.get(0);
    }
    if (request.paymentMethod() == PaymentMethod.DERIVED_ADDRESS) {
      return "switch_to_contract_or_hosted_wallet";
    }
    return "switch_to_derived_address_payment";
  }
}
