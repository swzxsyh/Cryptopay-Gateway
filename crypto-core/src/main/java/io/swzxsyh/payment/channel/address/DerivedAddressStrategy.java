package io.swzxsyh.payment.channel.address;

import java.util.List;

public interface DerivedAddressStrategy {

  DerivedAddressProvisionerMode mode();

  List<String> createAddresses(String chain, String token, int count, String orderNo);
}
