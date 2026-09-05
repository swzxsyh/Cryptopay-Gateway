package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.api.dto.ApiResponseCode;
import io.swzxsyh.payment.api.dto.CashierPaymentOrderView;
import io.swzxsyh.payment.api.dto.CashierChainOption;
import io.swzxsyh.payment.api.dto.CashierPageBootstrap;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.config.CryptoPaymentProperties.ChainProfile;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.domain.PaymentMethod;
import io.swzxsyh.payment.domain.PaymentOrder;
import io.swzxsyh.payment.chain.ChainFamilyResolver;
import io.swzxsyh.payment.merchant.MerchantPaymentChannelPolicyService;
import io.swzxsyh.payment.pending.PendingChainTransactionService;
import io.swzxsyh.payment.api.dto.PendingChainTransactionView;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.signature.CashierTokenService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 收银台入口控制器。 */
@Slf4j
@RestController
public class CashierPageController {

  private final CashierTokenService cashierTokenService;
  private final PaymentOrderService paymentOrderService;
  private final CryptoPaymentProperties properties;
  private final PendingChainTransactionService pendingTransactionService;
  private final MerchantPaymentChannelPolicyService merchantChannelPolicyService;

  public CashierPageController(
      CashierTokenService cashierTokenService,
      PaymentOrderService paymentOrderService,
      CryptoPaymentProperties properties,
      PendingChainTransactionService pendingTransactionService,
      MerchantPaymentChannelPolicyService merchantChannelPolicyService) {
    this.cashierTokenService = cashierTokenService;
    this.paymentOrderService = paymentOrderService;
    this.properties = properties;
    this.pendingTransactionService = pendingTransactionService;
    this.merchantChannelPolicyService = merchantChannelPolicyService;
  }

  /** 返回收银台首屏引导数据，由前端根据 statusMode 渲染收银台页面。 */
  @GetMapping("/cashier/{cashierToken}")
  public ApiResponse<CashierPageBootstrap> cashier(@PathVariable String cashierToken) {
    PaymentOrder order = resolveOrder(cashierToken);
    CashierPaymentOrderView view = view(order);
    CashierPageBootstrap bootstrap = new CashierPageBootstrap(
        cashierToken,
        resolveStatusMode(order),
        view,
        resolveSupportedChains(order),
        List.of(PaymentMethod.CONTRACT, PaymentMethod.DERIVED_ADDRESS)
    );
    return ApiResponse.ok(bootstrap);
  }

  private CashierPaymentOrderView view(PaymentOrder order) {
    PendingChainTransactionView pending =
        pendingTransactionService
            .findLatestByOrderNo(order.getCryptoOrderNo())
            .map(PendingChainTransactionView::from)
            .orElse(null);
    return CashierPaymentOrderView.from(order, pending);
  }

  private String resolveStatusMode(PaymentOrder order) {
    if (order.getStatus() == OrderStatus.UNDERPAID) {
      return "underpaid";
    }
    if (order.getStatus() == OrderStatus.OVERPAID) {
      return "overpaid";
    }
    if (order.isLatePayment()) {
      return "late";
    }
    return order.getStatus() == OrderStatus.PAID ? "paid" : "pay";
  }

  /** 缺少收银台 token 时返回统一错误响应。 */
  @GetMapping("/cashier")
  public ApiResponse<Void> missingToken() {
    return ApiResponse.fail(ApiResponseCode.UNAUTHORIZED, "missing cashier token");
  }

  private PaymentOrder resolveOrder(String cashierToken) {
    String cryptoOrderNo = cashierTokenService.resolveCryptoOrderNo(cashierToken);
    return paymentOrderService.getOrder(cryptoOrderNo);
  }

  /**
   * 解析收银台首屏可展示的链列表。
   * <p>
   * 规则是：
   * 1. 先查询商户已开启、平台链已开启、平台代币已开启的链币交集；
   * 2. 再与运行时 token profile 对齐，返回给前端真正可选的链；
   * 3. 如果订单已经绑定链但当前配置不可用，不再兜底展示，避免用户继续选择已关闭链路。
   */
  private List<CashierChainOption> resolveSupportedChains(PaymentOrder order) {
    String token = StringUtils.hasText(order.getCurrency()) ? order.getCurrency() : order.getToken();
    List<String> enabledMerchantChains = merchantChannelPolicyService.enabledChannels(order.getMerchantId(), token)
        .stream()
        .map(channel -> channel.getChainCode() == null ? "" : channel.getChainCode().trim().toUpperCase())
        .filter(StringUtils::hasText)
        .distinct()
        .toList();
    List<CashierChainOption> options = properties.getTokenProfiles().stream()
        .filter(Objects::nonNull)
        .filter(profile -> !StringUtils.hasText(token) || token.equalsIgnoreCase(profile.getToken()))
        .filter(profile -> enabledMerchantChains.contains(profile.getChain().toUpperCase()))
        .map(profile -> new CashierChainOption(
            profile.getChain(),
            ChainFamilyResolver.resolve(profile.getChain()).name(),
            profile.getToken(),
            profile.getTokenAddress(),
            profile.getDecimals(),
            resolveRpcUrl(profile.getChain())))
        .filter(option -> StringUtils.hasText(option.chain()))
        .collect(Collectors.toMap(
            option -> option.chain().toUpperCase() + "|" + Objects.toString(option.tokenAddress(), ""),
            option -> option,
            (left, right) -> left,
            LinkedHashMap::new))
        .values()
        .stream()
        .toList();

    if (!options.isEmpty()) {
      return options;
    }

    if (StringUtils.hasText(order.getChain())) {
      merchantChannelPolicyService.requireUsable(order.getMerchantId(), order.getChain(), token, order.getAmount());
      return List.of(new CashierChainOption(order.getChain(), ChainFamilyResolver.resolve(order.getChain()).name(), token, order.getTokenAddress(), null, resolveRpcUrl(order.getChain())));
    }

    return List.of();
  }

  private String resolveRpcUrl(String chain) {
    if (!StringUtils.hasText(chain) || properties.getChainProfiles().isEmpty()) {
      return null;
    }
    return properties.getChainProfiles().stream()
        .filter(Objects::nonNull)
        .filter(profile -> chain.equalsIgnoreCase(profile.getChain()))
        .map(ChainProfile::getRpcUrl)
        .filter(StringUtils::hasText)
        .findFirst()
        .orElse(null);
  }
}
