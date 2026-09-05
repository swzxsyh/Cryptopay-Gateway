package io.swzxsyh.payment.kyt;

/** KYT 提供方抽象。 */
public interface KytProvider {

  String providerId();

  KytProviderResult screen(KytScreeningRequest request);
}
