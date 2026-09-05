package io.swzxsyh.payment.api;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swzxsyh.payment.api.dto.ApiResponseCode;
import io.swzxsyh.payment.api.dto.TokenRoutePreviewRequest;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.exception.CashierAccessException;
import io.swzxsyh.payment.gas.GasPolicyRequest;
import io.swzxsyh.payment.kyt.KytScreeningRequest;
import io.swzxsyh.payment.mapper.PaymentTokenConfigMapper;
import io.swzxsyh.payment.merchant.MerchantPaymentChannelPolicyService;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import io.swzxsyh.payment.persistence.entity.PaymentTokenConfig;
import io.swzxsyh.payment.service.PaymentOrderService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 收银台预探测接口的订单绑定校验。
 *
 * <p>Cashier token 只证明“有这个订单的访问权”，这里再把请求里的 chain/token/tokenAddress
 * 收敛到该订单所属商户、订单币种和平台启用链币范围，避免合法 token 被当成任意链上探测 oracle。
 */
@Slf4j
@Service
public class CashierProbeGuardService {

  private final PaymentOrderService paymentOrderService;
  private final MerchantPaymentChannelPolicyService merchantChannelPolicyService;
  private final PaymentTokenConfigMapper tokenConfigMapper;
  private final HttpServletRequest request;

  public CashierProbeGuardService(
      PaymentOrderService paymentOrderService,
      MerchantPaymentChannelPolicyService merchantChannelPolicyService,
      PaymentTokenConfigMapper tokenConfigMapper,
      HttpServletRequest request) {
    this.paymentOrderService = paymentOrderService;
    this.merchantChannelPolicyService = merchantChannelPolicyService;
    this.tokenConfigMapper = tokenConfigMapper;
    this.request = request;
  }

  /** 校验支付路由预探测请求，并返回 token 绑定的订单。 */
  public PaymentOrder assertAllowed(TokenRoutePreviewRequest previewRequest) {
    return assertAllowed(
        previewRequest == null ? null : previewRequest.chain(),
        previewRequest == null ? null : previewRequest.token(),
        previewRequest == null ? null : previewRequest.tokenAddress(),
        null,
        "token route preview");
  }

  /** 校验 Gas 预探测请求，并返回 token 绑定的订单。 */
  public PaymentOrder assertAllowed(GasPolicyRequest gasRequest) {
    return assertAllowed(
        gasRequest == null ? null : gasRequest.chain(),
        gasRequest == null ? null : gasRequest.token(),
        gasRequest == null ? null : gasRequest.tokenAddress(),
        gasRequest == null ? null : gasRequest.amount(),
        "gas preview");
  }

  /** 校验 KYT 预探测请求，并返回 token 绑定的订单。 */
  public PaymentOrder assertAllowed(KytScreeningRequest kytRequest) {
    PaymentOrder order =
        assertAllowed(
            kytRequest == null ? null : kytRequest.chain(),
            kytRequest == null ? null : kytRequest.token(),
            kytRequest == null ? null : kytRequest.tokenAddress(),
            kytRequest == null ? null : kytRequest.amount(),
            "kyt screen");
    if (StringUtils.hasText(kytRequest == null ? null : kytRequest.cryptoOrderNo())
        && !order.getCryptoOrderNo().equalsIgnoreCase(kytRequest.cryptoOrderNo().trim())) {
      reject("kyt screen cryptoOrderNo does not match cashier token");
    }
    if (StringUtils.hasText(kytRequest == null ? null : kytRequest.merchantId())
        && !safeEquals(order.getMerchantId(), kytRequest.merchantId())) {
      reject("kyt screen merchantId does not match cashier token");
    }
    return order;
  }

  private PaymentOrder assertAllowed(
      String chain, String token, String tokenAddress, BigDecimal requestedAmount, String scene) {
    PaymentOrder order = currentCashierOrder();
    String normalizedChain = normalizeRequired(chain, scene + " chain is required");
    String normalizedToken = normalizeRequired(token, scene + " token is required");
    String expectedToken = normalizeRequired(order.getCurrency(), "order currency is missing");
    if (!expectedToken.equals(normalizedToken)) {
      log.warn("收银台预探测币种越权。scene={}, cryptoOrderNo={}, expectedToken={}, requestedToken={}",
          scene, order.getCryptoOrderNo(), expectedToken, normalizedToken);
      reject(scene + " token does not match order currency");
    }
    if (StringUtils.hasText(order.getChain()) && !order.getChain().trim().equalsIgnoreCase(normalizedChain)) {
      reject(scene + " chain does not match selected order route");
    }
    if (StringUtils.hasText(order.getToken()) && !order.getToken().trim().equalsIgnoreCase(normalizedToken)) {
      reject(scene + " token does not match selected order route");
    }
    if (requestedAmount != null
        && order.getAmount() != null
        && requestedAmount.compareTo(order.getAmount()) != 0) {
      reject(scene + " amount does not match order amount");
    }

    MerchantPaymentChannelConfig channel =
        merchantChannelPolicyService.usableChannel(
            order.getMerchantId(),
            normalizedChain,
            normalizedToken,
            order.getAmount());
    validateTokenAddress(scene, order, channel, normalizedChain, normalizedToken, tokenAddress);
    return order;
  }

  private PaymentOrder currentCashierOrder() {
    Object attr = request.getAttribute("cashierOrderNo");
    if (!(attr instanceof String) || !StringUtils.hasText((String) attr)) {
      reject("cashier order is missing");
    }
    String cryptoOrderNo = ((String) attr).trim();
    return paymentOrderService.getOrder(cryptoOrderNo);
  }

  private void validateTokenAddress(
      String scene,
      PaymentOrder order,
      MerchantPaymentChannelConfig channel,
      String chain,
      String token,
      String requestedTokenAddress) {
    PaymentTokenConfig tokenConfig =
        tokenConfigMapper.selectOne(
            Wrappers.<PaymentTokenConfig>lambdaQuery()
                .eq(PaymentTokenConfig::getChainCode, channel.getChainCode())
                .eq(PaymentTokenConfig::getTokenSymbol, channel.getTokenSymbol())
                .eq(PaymentTokenConfig::getEnabled, true)
                .last("LIMIT 1"));
    if (tokenConfig == null || !StringUtils.hasText(tokenConfig.getTokenAddress())) {
      reject(scene + " token config is not enabled");
    }
    String configuredAddress = normalizeAddress(tokenConfig.getTokenAddress());
    if (StringUtils.hasText(requestedTokenAddress)
        && !configuredAddress.equals(normalizeAddress(requestedTokenAddress))) {
      log.warn(
          "收银台预探测 tokenAddress 越权。scene={}, cryptoOrderNo={}, chain={}, token={}, expected={}, requested={}",
          scene,
          order.getCryptoOrderNo(),
          chain,
          token,
          configuredAddress,
          requestedTokenAddress);
      reject(scene + " tokenAddress is not allowed for this order");
    }
    if (StringUtils.hasText(order.getTokenAddress())
        && !configuredAddress.equals(normalizeAddress(order.getTokenAddress()))) {
      reject(scene + " tokenAddress does not match selected order route");
    }
  }

  private String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      reject(message);
    }
    return value.trim().toUpperCase();
  }

  private String normalizeAddress(String value) {
    if (value == null) {
      return "";
    }
    String trimmed = value.trim();
    return trimmed.startsWith("0x") || trimmed.startsWith("0X") ? trimmed.toLowerCase() : trimmed;
  }

  private boolean safeEquals(String left, String right) {
    return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
  }

  private void reject(String message) {
    throw new CashierAccessException(ApiResponseCode.UNAUTHORIZED, HttpStatus.FORBIDDEN, message);
  }
}
