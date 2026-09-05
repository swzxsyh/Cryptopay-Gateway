package io.swzxsyh.payment.config;

import io.swzxsyh.payment.api.CashierAccessInterceptor;
import io.swzxsyh.payment.gateway.x402.X402AccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers payment-application web interceptors.
 *
 * <p>Only the payment application owns cashier and x402 HTTP entry points, so interceptor path
 * registration lives here instead of the shared core module.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final CashierAccessInterceptor cashierAccessInterceptor;
  private final X402AccessInterceptor x402AccessInterceptor;

  /**
   * Creates the payment MVC configuration.
   *
   * @param cashierAccessInterceptor cashier-token interceptor
   * @param x402AccessInterceptor x402 API-key interceptor
   */
  public WebMvcConfig(
      CashierAccessInterceptor cashierAccessInterceptor,
      X402AccessInterceptor x402AccessInterceptor) {
    this.cashierAccessInterceptor = cashierAccessInterceptor;
    this.x402AccessInterceptor = x402AccessInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(cashierAccessInterceptor)
        .addPathPatterns(
            "/api/crypto/token-routing/preview",
            "/api/crypto/gas/preview",
            "/api/crypto/kyt/screen",
            "/api/crypto/orders/*/select-method",
            "/api/crypto/orders/*/tx-hash",
            "/api/crypto/evm/sponsor/orders/*/eip3009/submit",
            "/api/crypto/solana/orders/*/intent",
            "/api/crypto/solana/orders/*/fee-estimate",
            "/api/crypto/solana/orders/*/fee-payer-sign",
            "/api/crypto/solana/orders/*/send-raw-transaction");
    registry.addInterceptor(x402AccessInterceptor)
        .addPathPatterns("/api/crypto/x402/**");
  }
}
