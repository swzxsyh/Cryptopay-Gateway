package io.swzxsyh.payment.channel.address;

import io.swzxsyh.payment.channel.PaymentChannel;
import io.swzxsyh.payment.gas.GasPolicyPlanner;
import io.swzxsyh.payment.gas.GasPolicyRequest;
import io.swzxsyh.payment.gas.GasPolicyResult;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.domain.PaymentDetails;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;
import io.swzxsyh.payment.chain.ChainFamily;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DerivedAddressPaymentChannel implements PaymentChannel {

  private final CryptoPaymentProperties properties;
  private final GasPolicyPlanner gasPolicyPlanner;
  private final ObjectProvider<DerivedAddressPoolService> poolServiceProvider;
  private final ObjectProvider<DerivedAddressProvisioner> provisionerProvider;
  private final AtomicInteger cursor = new AtomicInteger();

  public DerivedAddressPaymentChannel(
      CryptoPaymentProperties properties,
      GasPolicyPlanner gasPolicyPlanner,
      ObjectProvider<DerivedAddressPoolService> poolServiceProvider,
      ObjectProvider<DerivedAddressProvisioner> provisionerProvider) {
    this.properties = properties;
    this.gasPolicyPlanner = gasPolicyPlanner;
    this.poolServiceProvider = poolServiceProvider;
    this.provisionerProvider = provisionerProvider;
  }

  @Override
  public PaymentMethod method() {
    return PaymentMethod.DERIVED_ADDRESS;
  }

  @Override
  public boolean supports(String chain, String token) {
    ChainFamily family = ChainFamilyResolver.resolve(chain);
    return properties.getDerivedAddress().isEnabled()
        && (family == ChainFamily.EVM || family == ChainFamily.SOLANA);
  }

  @Override
  public PaymentDetails prepare(PaymentOrder order, PaymentSelection selection) {
    DerivedAddressLease lease = leaseAddress(order, selection);
    GasPolicyResult gasResult = gasPolicyPlanner.plan(new GasPolicyRequest(
        selection.chain(),
        selection.token(),
        selection.tokenAddress(),
        selection.walletAddress(),
        selection.walletAccountType(),
        method(),
        null,
        order.getAmount()
    ));

    log.info("Allocated derived address payment order. cryptoOrderNo={}, chain={}, token={}, address={}, leaseId={}, poolMode={}",
        order.getCryptoOrderNo(), selection.chain(), selection.token(), lease.address(), lease.leaseId(),
        lease.autoCreated() ? "auto-create" : "queue");

    return new PaymentDetails(
        method(),
        selection.chain(),
        selection.token(),
        selection.tokenAddress(),
        selection.walletAddress(),
        selection.walletAccountType(),
        null,
        "derived-address route",
        lease.address(),
        null,
        null,
        gasResult.payerMode().name(),
        gasResult.reason(),
        gasResult.estimatedNativeFeeWei(),
        gasResult.customerBalanceSufficient(),
        gasResult.platformBalanceSufficient(),
        gasResult.fallbackSuggestion(),
        lease.poolKey(),
        lease.leaseId()
    );
  }

  private DerivedAddressLease leaseAddress(PaymentOrder order, PaymentSelection selection) {
    DerivedAddressPoolService poolService = poolServiceProvider.getIfAvailable();
    if (poolService != null) {
      return poolService.lease(selection.chain(), selection.token(), order.getCryptoOrderNo());
    }

    DerivedAddressProvisioner provisioner = provisionerProvider.getIfAvailable();
    if (provisioner != null) {
      String address = provisioner.createAddresses(selection.chain(), selection.token(), 1, order.getCryptoOrderNo()).get(0);
      return new DerivedAddressLease(
          "local:" + selection.chain() + ":" + selection.token(),
          "fallback-" + order.getCryptoOrderNo(),
          address,
          order.getCryptoOrderNo(),
          true,
          java.time.LocalDateTime.now(),
          java.time.LocalDateTime.now().plusMinutes(properties.getOrderExpireMinutes())
      );
    }

    return new DerivedAddressLease(
        "local:" + selection.chain() + ":" + selection.token(),
        "fallback-" + order.getCryptoOrderNo(),
        allocateAddress(),
        order.getCryptoOrderNo(),
        false,
        java.time.LocalDateTime.now(),
        java.time.LocalDateTime.now().plusMinutes(properties.getOrderExpireMinutes())
    );
  }

  private String allocateAddress() {
    return "0x" + String.format("%040x", cursor.incrementAndGet());
  }
}
