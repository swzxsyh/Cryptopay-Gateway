package io.swzxsyh.payment.api.dto;

import io.swzxsyh.payment.domain.PaymentMethod;
import java.util.List;

/** 收银台页面首屏注入数据。 */
public record CashierPageBootstrap(
    String cashierToken,
    String statusMode,
    CashierPaymentOrderView order,
    List<CashierChainOption> supportedChains,
    List<PaymentMethod> supportedPaymentMethods
) {
}
