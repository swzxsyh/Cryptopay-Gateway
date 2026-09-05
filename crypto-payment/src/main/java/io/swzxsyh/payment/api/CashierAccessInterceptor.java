package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponseCode;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.exception.CashierAccessException;
import io.swzxsyh.payment.service.PaymentOrderService;
import io.swzxsyh.payment.signature.CashierTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Validates cashier-token protected payment APIs.
 *
 * <p>The interceptor belongs to the payment application because it protects user-facing cashier
 * actions, such as method selection, gas preview, wallet reporting, and Solana transaction
 * preparation.
 */
@Slf4j
@Component
public class CashierAccessInterceptor implements HandlerInterceptor {

  public static final String HEADER_TOKEN = "X-Cashier-Token";

  private final CashierTokenService cashierTokenService;
  private final PaymentOrderService paymentOrderService;

  /**
   * Creates a cashier access interceptor.
   *
   * @param cashierTokenService cashier token resolver
   * @param paymentOrderService payment order query service
   */
  public CashierAccessInterceptor(
      CashierTokenService cashierTokenService,
      PaymentOrderService paymentOrderService) {
    this.cashierTokenService = cashierTokenService;
    this.paymentOrderService = paymentOrderService;
  }

  /**
   * Resolves the cashier token, checks that it matches the path order, and rejects terminal orders.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @param handler selected handler
   * @return true if the request can continue
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String token = request.getHeader(HEADER_TOKEN);
    if (!StringUtils.hasText(token)) {
      throw new CashierAccessException(ApiResponseCode.UNAUTHORIZED, HttpStatus.FORBIDDEN, "cashier token is required");
    }

    String cryptoOrderNo;
    try {
      cryptoOrderNo = cashierTokenService.resolveCryptoOrderNo(token);
    } catch (IllegalArgumentException ex) {
      throw new CashierAccessException(ApiResponseCode.NOT_FOUND, HttpStatus.NOT_FOUND, "invalid cashier token");
    }

    var order = paymentOrderService.getOrder(cryptoOrderNo);
    String pathOrderNo = extractOrderNoFromApiPath(request);
    if (StringUtils.hasText(pathOrderNo) && !cryptoOrderNo.equalsIgnoreCase(pathOrderNo.trim())) {
      throw new CashierAccessException(ApiResponseCode.UNAUTHORIZED, HttpStatus.FORBIDDEN, "cashier token does not match order");
    }
    request.setAttribute("cashierOrderNo", cryptoOrderNo);
    request.setAttribute("cashierToken", token);
    request.setAttribute("merchantId", order.getMerchantId());
    if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
      throw new CashierAccessException(ApiResponseCode.GONE, HttpStatus.GONE, "cashier order expired");
    }
    if (order.getStatus() == OrderStatus.EXPIRED
        || order.getStatus() == OrderStatus.CANCELLED
        || order.getStatus() == OrderStatus.UNDERPAID
        || order.getStatus() == OrderStatus.OVERPAID
        || order.getStatus() == OrderStatus.PAID) {
      throw new CashierAccessException(ApiResponseCode.GONE, HttpStatus.GONE, "cashier order is not payable");
    }
    return true;
  }

  private String extractOrderNoFromApiPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (!StringUtils.hasText(path)) {
      return null;
    }
    String[] markers = {
        "/api/crypto/orders/",
        "/api/crypto/solana/orders/",
        "/api/crypto/evm/sponsor/orders/"
    };
    String marker = null;
    int start = -1;
    for (String candidate : markers) {
      start = path.indexOf(candidate);
      if (start >= 0) {
        marker = candidate;
        break;
      }
    }
    if (marker == null) {
      return null;
    }
    String remainder = path.substring(start + marker.length());
    int slash = remainder.indexOf('/');
    return slash < 0 ? remainder : remainder.substring(0, slash);
  }
}
