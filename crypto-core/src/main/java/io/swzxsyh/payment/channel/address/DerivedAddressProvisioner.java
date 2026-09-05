package io.swzxsyh.payment.channel.address;

import java.util.List;

/** 派生地址生成器抽象。 */
public interface DerivedAddressProvisioner {

  List<String> createAddresses(String chain, String token, int count, String orderNo);
}
