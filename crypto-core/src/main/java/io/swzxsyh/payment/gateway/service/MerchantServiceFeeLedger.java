package io.swzxsyh.payment.gateway.service;

import io.swzxsyh.payment.gateway.model.GatewayFeeReceipt;
import io.swzxsyh.payment.mapper.MerchantServiceFeeAccountMapper;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MerchantServiceFeeLedger {

  private final MerchantServiceFeeAccountMapper mapper;

  public MerchantServiceFeeLedger(MerchantServiceFeeAccountMapper mapper) {
    this.mapper = mapper;
  }

  @Transactional
  public BigDecimal credit(String merchantId, String feeToken, BigDecimal amount) {
    validate(merchantId, feeToken, amount);
    mapper.credit(merchantId, feeToken, amount);
    return getBalance(merchantId, feeToken);
  }

  @Transactional
  public GatewayFeeReceipt deduct(String merchantId, String feeToken, BigDecimal amount) {
    validate(merchantId, feeToken, amount);
    int updated = mapper.deduct(merchantId, feeToken, amount);
    if (updated == 0) {
      throw new IllegalStateException("merchant service fee balance is insufficient");
    }

    return new GatewayFeeReceipt(
        nextReceiptNo(),
        merchantId,
        feeToken,
        amount,
        getBalance(merchantId, feeToken)
    );
  }

  public BigDecimal getBalance(String merchantId, String feeToken) {
    if (!StringUtils.hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required");
    }
    if (!StringUtils.hasText(feeToken)) {
      throw new IllegalArgumentException("feeToken is required");
    }
    BigDecimal balance = mapper.selectBalance(merchantId, feeToken);
    return balance == null ? BigDecimal.ZERO : balance;
  }

  private void validate(String merchantId, String feeToken, BigDecimal amount) {
    if (!StringUtils.hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required");
    }
    if (!StringUtils.hasText(feeToken)) {
      throw new IllegalArgumentException("feeToken is required");
    }
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("fee amount must be greater than zero");
    }
  }

  private BigDecimal safe(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private String nextReceiptNo() {
    return "FEE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
  }
}
