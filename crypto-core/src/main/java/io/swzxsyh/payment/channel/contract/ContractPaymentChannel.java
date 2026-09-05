package io.swzxsyh.payment.channel.contract;

import io.swzxsyh.payment.channel.PaymentChannel;
import io.swzxsyh.payment.gas.GasPolicyPlanner;
import io.swzxsyh.payment.gas.GasPolicyRequest;
import io.swzxsyh.payment.gas.GasPolicyResult;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentDetails;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.settlement.ContractSettlementCommand;
import io.swzxsyh.payment.settlement.ContractSettlementPlan;
import io.swzxsyh.payment.settlement.ContractSettlementPlanBuilder;
import io.swzxsyh.payment.settlement.SettlementRule;
import io.swzxsyh.payment.settlement.record.ContractSettlementRecordService;
import io.swzxsyh.payment.routing.TokenRoutePlan;
import io.swzxsyh.payment.routing.TokenRoutePlanner;
import io.swzxsyh.payment.routing.TokenRouteRequest;
import io.swzxsyh.payment.routing.TokenRouteType;
import io.swzxsyh.payment.sponsor.GasSponsorContext;
import io.swzxsyh.payment.sponsor.GasSponsorPlan;
import io.swzxsyh.payment.sponsor.GasSponsorRegistry;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContractPaymentChannel implements PaymentChannel {

  private final CryptoPaymentProperties properties;
  private final ContractSettlementPlanBuilder settlementPlanBuilder;
  private final ContractSettlementRecordService settlementRecordService;
  private final TokenRoutePlanner tokenRoutePlanner;
  private final GasPolicyPlanner gasPolicyPlanner;
  private final ContractAddressPlanner contractAddressPlanner;
  private final GasSponsorRegistry gasSponsorRegistry;

  public ContractPaymentChannel(
      CryptoPaymentProperties properties,
      ContractSettlementPlanBuilder settlementPlanBuilder,
      ContractSettlementRecordService settlementRecordService,
      TokenRoutePlanner tokenRoutePlanner,
      GasPolicyPlanner gasPolicyPlanner,
      ContractAddressPlanner contractAddressPlanner,
      GasSponsorRegistry gasSponsorRegistry) {
    this.properties = properties;
    this.settlementPlanBuilder = settlementPlanBuilder;
    this.settlementRecordService = settlementRecordService;
    this.tokenRoutePlanner = tokenRoutePlanner;
    this.gasPolicyPlanner = gasPolicyPlanner;
    this.contractAddressPlanner = contractAddressPlanner;
    this.gasSponsorRegistry = gasSponsorRegistry;
  }

  @Override
  public PaymentMethod method() {
    return PaymentMethod.CONTRACT;
  }

  @Override
  public boolean supports(String chain, String token) {
    return properties.getContract().isEnabled() && ChainFamilyResolver.resolve(chain) == ChainFamily.EVM;
  }

  @Override
  public PaymentDetails prepare(PaymentOrder order, PaymentSelection selection) {
    ContractAddressPlan contractAddressPlan = contractAddressPlanner.resolve(order, selection);
    String contractAddress = contractAddressPlan.address();
    TokenRoutePlan routePlan = tokenRoutePlanner.plan(new TokenRouteRequest(
        selection.chain(),
        selection.token(),
        selection.tokenAddress(),
        selection.walletAddress(),
        selection.walletAccountType()
    ));
    GasPolicyResult gasResult = gasPolicyPlanner.plan(new GasPolicyRequest(
        selection.chain(),
        selection.token(),
        selection.tokenAddress(),
        selection.walletAddress(),
        selection.walletAccountType(),
        method(),
        routePlan.routeType(),
        order.getAmount()
    ));

    String callData;
    String routeReason = routePlan.routeReason();
    TokenRouteType routeType = TokenRouteType.SMART_CONTRACT_SETTLEMENT;
    String settlementContractAddress = null;
    GasSponsorPlan sponsorPlan =
        gasSponsorRegistry.plan(new GasSponsorContext(order, selection, routePlan, gasResult));

    if (sponsorPlan.available() && routePlan.routeType() == TokenRouteType.TRANSFER_WITH_AUTHORIZATION) {
      // EIP-3009 路线不下发合约 calldata，前端需要让用户签 EIP-712 授权，再交给后端 relayer 上链代付。
      routeType = TokenRouteType.TRANSFER_WITH_AUTHORIZATION;
      settlementContractAddress = contractAddress;
      callData = null;
      routeReason = "hosted wallet EIP-3009 sponsored payment"
          + "; sponsorProvider=" + sponsorPlan.providerId()
          + "; sponsorPayload=" + sponsorPlan.payload()
          + "; contractAddressMode=" + contractAddressPlan.mode()
          + "; tokenRouteReason=" + routeReason;
      log.info("Prepared hosted wallet EIP-3009 sponsored payment. cryptoOrderNo={}, chain={}, token={}, escrow={}",
          order.getCryptoOrderNo(), selection.chain(), selection.token(), contractAddress);
      return new PaymentDetails(
          method(),
          selection.chain(),
          selection.token(),
          selection.tokenAddress(),
          selection.walletAddress(),
          routePlan.walletAccountType(),
          routeType,
          routeReason,
          null,
          settlementContractAddress,
          callData,
          sponsorPlan.payerMode().name(),
          sponsorPlan.reason(),
          gasResult.estimatedNativeFeeWei(),
          gasResult.customerBalanceSufficient(),
          gasResult.platformBalanceSufficient(),
          gasResult.fallbackSuggestion(),
          null,
          null
      );
    }

    // 托管钱包入口只下发合约调用，不直接把 EIP-3009/permit/approve 的说明字符串交给前端当 calldata。
    // tokenRoutePlanner 的结果作为能力参考保留在 routeReason 中，真实支付动作统一进入 payIntoEscrow。
    settlementContractAddress = contractAddress;
    ContractSettlementPlan plan = settlementPlanBuilder.build(new ContractSettlementCommand(
        order.getCryptoOrderNo(),
        selection.chain(),
        selection.token(),
        selection.tokenAddress(),
        tokenDecimals(selection.chain(), selection.token()),
        contractAddress,
        order.getAmount(),
        order.getExpireTime(),
        configuredSettlementRules()
    ));
    settlementRecordService.recordPlanned(order, plan);
    callData = plan.contractCallData();
    routeReason = "hosted wallet escrow payment"
        + "; tokenRouteCandidate=" + routePlan.routeType()
        + "; tokenRouteReason=" + routeReason
        + "; contractAddressMode=" + contractAddressPlan.mode()
        + "; " + plan.signPayload();
    log.info("Prepared hosted wallet escrow payment order. cryptoOrderNo={}, chain={}, token={}, contract={}, routeCandidate={}, splits={}",
        order.getCryptoOrderNo(), selection.chain(), selection.token(), contractAddress, routePlan.routeType(), plan.splits().size());

    return new PaymentDetails(
        method(),
        selection.chain(),
        selection.token(),
        selection.tokenAddress(),
        selection.walletAddress(),
        routePlan.walletAccountType(),
        routeType,
        routeReason,
        null,
        settlementContractAddress,
        callData,
        gasResult.payerMode().name(),
        gasResult.reason(),
        gasResult.estimatedNativeFeeWei(),
        gasResult.customerBalanceSufficient(),
        gasResult.platformBalanceSufficient(),
        gasResult.fallbackSuggestion(),
        null,
        null
    );
  }

  private List<SettlementRule> configuredSettlementRules() {
    return properties.getContract().getSettlementRules().stream()
        .map(rule -> new SettlementRule(rule.getRole(), rule.getReceiver(), rule.getBasisPoints()))
        .toList();
  }

  private int tokenDecimals(String chain, String token) {
    return properties.getTokenProfiles().stream()
        .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
        .filter(profile -> token.equalsIgnoreCase(profile.getToken()))
        .findFirst()
        .map(CryptoPaymentProperties.TokenProfile::getDecimals)
        .orElse(6);
  }
}
