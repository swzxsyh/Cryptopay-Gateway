package io.swzxsyh.payment.channel;

import io.swzxsyh.payment.domain.PaymentDetails;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.domain.PaymentSelection;

public interface PaymentChannel {

  PaymentMethod method();

  boolean supports(String chain, String token);

  PaymentDetails prepare(PaymentOrder order, PaymentSelection selection);
}
