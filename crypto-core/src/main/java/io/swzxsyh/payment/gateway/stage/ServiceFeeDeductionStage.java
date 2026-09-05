package io.swzxsyh.payment.gateway.stage;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.gateway.model.GatewayContext;
import io.swzxsyh.payment.gateway.model.GatewayFeeReceipt;
import io.swzxsyh.payment.gateway.service.MerchantServiceFeeLedger;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 网关服务费扣减阶段。 */
@Slf4j
@Component
@Order(10)
public class ServiceFeeDeductionStage implements GatewayStage {

  private final CryptoPaymentProperties properties;
  private final MerchantServiceFeeLedger ledger;

  public ServiceFeeDeductionStage(
      CryptoPaymentProperties properties,
      MerchantServiceFeeLedger ledger) {
    this.properties = properties;
    this.ledger = ledger;
  }

  @Override
  public void apply(GatewayContext context) {
    if (!properties.getGateway().isEnabled()) {
      log.info("Gateway fee layer disabled, skipping deduction.");
      return;
    }

    String merchantId = context.getRequest().merchantId();
    if (!StringUtils.hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required for gateway billing");
    }

    String feeToken = properties.getGateway().getServiceFeeToken();
    BigDecimal feeAmount = properties.getGateway().getServiceFeeAmount();
    GatewayFeeReceipt receipt = ledger.deduct(merchantId, feeToken, feeAmount);
    context.setFeeReceipt(receipt);
    log.info("Deducted gateway fee. merchantId={}, feeToken={}, feeAmount={}, remainingBalance={}, receiptNo={}",
        merchantId, feeToken, feeAmount, receipt.remainingBalance(), receipt.receiptNo());
  }
}
