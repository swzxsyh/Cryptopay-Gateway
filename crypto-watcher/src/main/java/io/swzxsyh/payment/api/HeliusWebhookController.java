package io.swzxsyh.payment.api;

import io.swzxsyh.payment.api.dto.ApiResponse;
import io.swzxsyh.payment.application.HeliusWebhookIngestionService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Watcher 侧 Helius webhook 接收入口，只负责把 Helius 推送转换为内部链上入账消息。 */
@Slf4j
@RestController
@RequestMapping("/api/crypto/solana")
public class HeliusWebhookController {

  private final HeliusWebhookIngestionService heliusWebhookIngestionService;
  private final CryptoPaymentProperties properties;

  public HeliusWebhookController(
      HeliusWebhookIngestionService heliusWebhookIngestionService,
      CryptoPaymentProperties properties) {
    this.heliusWebhookIngestionService = heliusWebhookIngestionService;
    this.properties = properties;
  }

  /** Helius Webhook 接收入口；启用 secret 时需携带配置的 header。 */
  @PostMapping("/helius/webhook/{chain}")
  public ApiResponse<Integer> heliusWebhook(
      @PathVariable String chain,
      @RequestBody JsonNode payload,
      HttpServletRequest request) {
    verifyHeliusSecret(request);
    int count = heliusWebhookIngestionService.ingest(chain, payload);
    log.info("Helius webhook handled. chain={}, eventCount={}", chain, count);
    return ApiResponse.ok(count);
  }

  private void verifyHeliusSecret(HttpServletRequest request) {
    if (!properties.getSolana().isHeliusWebhookEnabled()) {
      throw new IllegalStateException("Helius webhook is disabled");
    }
    String expected = properties.getSolana().getHeliusWebhookSecret();
    if (!StringUtils.hasText(expected)) {
      return;
    }
    String headerName = StringUtils.hasText(properties.getSolana().getHeliusWebhookSecretHeader())
        ? properties.getSolana().getHeliusWebhookSecretHeader()
        : "X-Helius-Webhook-Secret";
    String actual = request.getHeader(headerName);
    if (!expected.equals(actual)) {
      throw new IllegalArgumentException("Invalid Helius webhook secret");
    }
  }
}
