package io.swzxsyh.payment.util;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 发送 Telegram 文本消息。 */
@Slf4j
@Service
public class TelegramMessageSender {

  private final HttpClientUtil httpClientUtil;
  private final boolean enabled;
  private final String botToken;
  private final String apiUrl;
  private final String defaultChatId;

  public TelegramMessageSender(
      HttpClientUtil httpClientUtil,
      @Value("${crypto.payment.alert.telegram.enabled:false}") boolean enabled,
      @Value("${crypto.payment.alert.telegram.bot-token:}") String botToken,
      @Value("${crypto.payment.alert.telegram.api-url:https://api.telegram.org/bot/}") String apiUrl,
      @Value("${crypto.payment.alert.telegram.chat-id:}") String defaultChatId) {
    this.httpClientUtil = httpClientUtil;
    this.enabled = enabled;
    this.botToken = botToken;
    this.apiUrl = apiUrl.endsWith("/") ? apiUrl : apiUrl + "/";
    this.defaultChatId = defaultChatId;
  }

  /** 向默认群组发送告警。 */
  public boolean sendMessage(String text) {
    return sendMessage(defaultChatId, text);
  }

  /** 向指定 Telegram chatId 发送文本。 */
  public boolean sendMessage(String chatId, String text) {
    if (!enabled || !StringUtils.hasText(botToken) || !StringUtils.hasText(chatId) || !StringUtils.hasText(text)) {
      return false;
    }

    try {
      String url = apiUrl + botToken + "/sendMessage";
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("chat_id", chatId);
      payload.put("text", text);
      payload.put("disable_web_page_preview", true);

      HttpClientUtil.ExternalHttpResponse response = httpClientUtil.postJson(url, payload, Map.of());
      if (!response.is2xxSuccessful()) {
        log.warn("Telegram message send returned non-2xx. chatId={}, httpStatus={}, response={}",
            chatId, response.statusCode(), response.body());
        return false;
      }
      log.info("Telegram message sent. chatId={}, response={}", chatId, response.body());
      return true;
    } catch (Exception e) {
      log.warn("Telegram message send failed. chatId={}, error={}", chatId, e.getMessage());
      return false;
    }
  }
}
