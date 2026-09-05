package io.swzxsyh.payment.sponsor;

import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.gas.GasPayerMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Solana 平台 Fee Payer 代付规划，真实签名仍由 SolanaPaymentService 的白名单接口完成。 */
@Component
public class SolanaFeePayerGasSponsorProvider implements GasSponsorProvider {

  private final CryptoPaymentProperties properties;
  private final GasSponsorshipPolicyService policyService;

  public SolanaFeePayerGasSponsorProvider(
      CryptoPaymentProperties properties,
      GasSponsorshipPolicyService policyService) {
    this.properties = properties;
    this.policyService = policyService;
  }

  @Override
  public String providerId() {
    return "SOLANA_FEE_PAYER";
  }

  @Override
  public boolean supports(GasSponsorContext context) {
    if (context == null || context.selection() == null) {
      return false;
    }
    return ChainFamilyResolver.resolve(context.selection().chain()) == ChainFamily.SOLANA
        && context.selection().paymentMethod() == PaymentMethod.CONTRACT
        && policyService.isSponsorEnabled(context.selection().chain(), providerId())
        && StringUtils.hasText(properties.getSolana().getFeePayerAddress());
  }

  @Override
  public GasSponsorPlan plan(GasSponsorContext context) {
    String payload = "{\"feePayerAddress\":\"" + properties.getSolana().getFeePayerAddress()
        + "\",\"signEndpoint\":\"/api/crypto/solana/orders/"
        + context.order().getCryptoOrderNo() + "/fee-payer-sign\"}";
    return new GasSponsorPlan(
        true,
        true,
        providerId(),
        GasPayerMode.PLATFORM_SPONSORED,
        "Solana fee payer enabled for this chain",
        true,
        "SOLANA_PARTIAL_TRANSACTION",
        payload);
  }
}
