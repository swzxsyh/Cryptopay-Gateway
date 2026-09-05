package io.swzxsyh.payment.alert;

import io.swzxsyh.payment.util.TelegramMessageSender;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 统一告警服务。 */
@Slf4j
@Service
public class PaymentAlertService {

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final long DEFAULT_COOLDOWN_MILLIS = 10 * 60 * 1000L;

  private final TelegramMessageSender telegramMessageSender;
  private final Map<String, Long> lastSentAt = new ConcurrentHashMap<>();

  public PaymentAlertService(TelegramMessageSender telegramMessageSender) {
    this.telegramMessageSender = telegramMessageSender;
  }

  public void alertCallbackDeadLetter(String cryptoOrderNo, String merchantOrderNo, String callbackUrl, String error) {
    sendThrottled(
        "callback-dead-letter",
        buildMessage("Callback Dead Letter", fields(
            "cryptoOrderNo", cryptoOrderNo,
            "merchantOrderNo", merchantOrderNo,
            "callbackUrl", callbackUrl,
            "error", error)));
  }

  public void alertScanFailure(String chain, String error) {
    sendThrottled(
        "scan-failure:" + safe(chain),
        buildMessage("Chain Scan Failure", fields("chain", chain, "error", error)));
  }

  public void alertWebSocketFailure(String chain, String error) {
    sendThrottled(
        "ws-failure:" + safe(chain),
        buildMessage("WebSocket Observer Failure", fields("chain", chain, "error", error)));
  }

  public void alertWebSocketLeaderAcquired(String chain, String instanceId) {
    sendThrottled(
        "ws-leader-acquired:" + safe(chain) + ":" + safe(instanceId),
        buildMessage("WebSocket Leader Acquired", fields("chain", chain, "instanceId", instanceId)));
  }

  public void alertWebSocketLeaderReleased(String chain, String instanceId, String reason) {
    sendThrottled(
        "ws-leader-released:" + safe(chain) + ":" + safe(instanceId),
        buildMessage(
            "WebSocket Leader Released",
            fields("chain", chain, "instanceId", instanceId, "reason", reason)));
  }

  public void alertPaymentInstanceStarted(String instanceId, int chainCount) {
    sendThrottled(
        "payment-instance-started:" + safe(instanceId),
        buildMessage("Payment Instance Started", fields("instanceId", instanceId, "chainCount", chainCount)));
  }

  public void alertPaymentInstanceStopped(String instanceId) {
    sendThrottled(
        "payment-instance-stopped:" + safe(instanceId),
        buildMessage("Payment Instance Stopped", fields("instanceId", instanceId)));
  }

  public void alertKytRejected(String cryptoOrderNo, String chain, String token, String payeeAddress, int riskScore, String providerId) {
    sendThrottled(
        "kyt-reject:" + safe(chain) + ":" + safe(token),
        buildMessage("KYT Rejected", fields(
            "cryptoOrderNo", cryptoOrderNo,
            "chain", chain,
            "token", token,
            "payeeAddress", payeeAddress,
            "riskScore", riskScore,
            "provider", providerId)));
  }

  public void alertMissingChainProfiles() {
    sendThrottled("missing-chain-profiles", buildMessage("Scanner Idle", fields("message", "No chain profile configured")));
  }

  private void sendThrottled(String key, String message) {
    if (!StringUtils.hasText(message)) {
      return;
    }
    long now = System.currentTimeMillis();
    long last = lastSentAt.getOrDefault(key, 0L);
    if (now - last < DEFAULT_COOLDOWN_MILLIS) {
      return;
    }
    if (telegramMessageSender.sendMessage(message)) {
      lastSentAt.put(key, now);
    }
  }

  private String buildMessage(String title, Map<String, ?> fields) {
    StringBuilder sb = new StringBuilder();
    sb.append(title).append('\n');
    sb.append("time=").append(LocalDateTime.now().format(TIME_FORMAT)).append('\n');
    String requestId = MDC.get("requestId");
    if (StringUtils.hasText(requestId)) {
      sb.append("requestId=").append(requestId).append('\n');
    }
    for (Map.Entry<String, ?> entry : fields.entrySet()) {
      sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
    }
    return sb.toString().trim();
  }

  private Map<String, Object> fields(Object... values) {
    Map<String, Object> result = new LinkedHashMap<>();
    if (values == null) {
      return result;
    }
    for (int i = 0; i + 1 < values.length; i += 2) {
      String key = String.valueOf(values[i]);
      result.put(key, values[i + 1]);
    }
    return result;
  }

  private String safe(String value) {
    return StringUtils.hasText(value) ? value : "-";
  }
}
