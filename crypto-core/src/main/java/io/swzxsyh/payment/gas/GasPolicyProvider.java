package io.swzxsyh.payment.gas;

public interface GasPolicyProvider {

  String providerId();

  GasPolicyResult plan(GasPolicyRequest request);
}
