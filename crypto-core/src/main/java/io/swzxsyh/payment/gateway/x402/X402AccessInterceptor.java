package io.swzxsyh.payment.gateway.x402;

import io.swzxsyh.payment.config.CryptoPaymentProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/** X402 资源访问门禁。 */
@Slf4j
@Component
public class X402AccessInterceptor implements HandlerInterceptor {

  public static final String HEADER_API_KEY = "X-API-Key";

  private final CryptoPaymentProperties properties;

  public X402AccessInterceptor(CryptoPaymentProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String path = request.getRequestURI().toLowerCase(Locale.ROOT);
    if (path.endsWith("/discovery") || path.endsWith("/support")) {
      if (properties.getGateway().isPublicResourcesEnabled()) {
        return true;
      }
    }

    String expected = properties.getGateway().getApiKey();
    if (!StringUtils.hasText(expected)) {
      return true;
    }

    String actual = request.getHeader(HEADER_API_KEY);
    if (!StringUtils.hasText(actual) || !expected.equals(actual)) {
      log.warn("Rejected x402 request due to missing or invalid API key. path={}", request.getRequestURI());
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      return false;
    }
    return true;
  }
}
