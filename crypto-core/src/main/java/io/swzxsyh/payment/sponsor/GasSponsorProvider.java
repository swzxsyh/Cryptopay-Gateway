package io.swzxsyh.payment.sponsor;

/** Gas 代付 provider 抽象，后续可扩展 EIP-4337 Paymaster、第三方 relayer、新链 fee payer。 */
public interface GasSponsorProvider {

  String providerId();

  boolean supports(GasSponsorContext context);

  GasSponsorPlan plan(GasSponsorContext context);

  default GasSponsorExecutionResult execute(GasSponsorExecutionCommand command) {
    throw new UnsupportedOperationException(providerId() + " does not support server-side execution");
  }
}
