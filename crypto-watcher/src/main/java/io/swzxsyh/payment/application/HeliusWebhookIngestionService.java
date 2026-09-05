package io.swzxsyh.payment.application;

import io.swzxsyh.payment.messaging.ChainPaymentEvent;
import io.swzxsyh.payment.messaging.ChainPaymentEventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Helius Webhook 入账解析器，把 Helius 推送转换为内部统一链上入账事件。 */
@Slf4j
@Service
public class HeliusWebhookIngestionService {

  private static final BigDecimal LAMPORTS_PER_SOL = new BigDecimal("1000000000");

  private final ChainPaymentEventPublisher eventPublisher;

  public HeliusWebhookIngestionService(ChainPaymentEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /** 解析 Helius enhanced transaction webhook，返回发布的内部事件数量。 */
  public int ingest(String chain, JsonNode payload) {
    if (payload == null || payload.isMissingNode()) {
      return 0;
    }
    int count = 0;
    if (payload.isArray()) {
      for (JsonNode item : payload) {
        count += ingestTransaction(chain, item);
      }
      return count;
    }
    return ingestTransaction(chain, payload);
  }

  private int ingestTransaction(String chain, JsonNode tx) {
    String signature = tx.path("signature").asText(tx.path("transactionSignature").asText(""));
    if (!StringUtils.hasText(signature)) {
      return 0;
    }
    long slot = tx.path("slot").asLong(tx.path("blockNumber").asLong(0L));
    Instant timestamp = parseTimestamp(tx);
    int count = 0;
    for (JsonNode transfer : tx.path("tokenTransfers")) {
      String destination = transfer.path("toUserAccount").asText(transfer.path("toTokenAccount").asText(""));
      if (!StringUtils.hasText(destination)) {
        continue;
      }
      ChainPaymentEvent event =
          ChainPaymentEvent.of(
              "HELIUS_SPL_WEBHOOK",
              chain,
              null,
              transfer.path("mint").asText(""),
              signature,
              transfer.path("fromUserAccount").asText(transfer.path("fromTokenAccount").asText("")),
              destination,
              parseAmount(transfer.path("tokenAmount").asText("0")),
              slot <= 0L ? null : slot,
              nextLogIndex(count),
              timestamp);
      eventPublisher.publish(event);
      count++;
    }
    for (JsonNode transfer : tx.path("nativeTransfers")) {
      String destination = transfer.path("toUserAccount").asText("");
      if (!StringUtils.hasText(destination)) {
        continue;
      }
      BigDecimal amount =
          parseAmount(transfer.path("amount").asText("0"))
              .divide(LAMPORTS_PER_SOL, 9, RoundingMode.DOWN);
      ChainPaymentEvent event =
          ChainPaymentEvent.of(
              "HELIUS_SOL_WEBHOOK",
              chain,
              "SOL",
              null,
              signature,
              transfer.path("fromUserAccount").asText(""),
              destination,
              amount,
              slot <= 0L ? null : slot,
              nextLogIndex(count),
              timestamp);
      eventPublisher.publish(event);
      count++;
    }
    if (count > 0) {
      log.info("Helius webhook parsed. chain={}, signature={}, eventCount={}", chain, signature, count);
    }
    return count;
  }

  private Long nextLogIndex(int count) {
    return (long) count;
  }

  private Instant parseTimestamp(JsonNode tx) {
    if (tx.hasNonNull("timestamp")) {
      long value = tx.path("timestamp").asLong();
      return value > 0L ? Instant.ofEpochSecond(value) : null;
    }
    if (tx.hasNonNull("blockTime")) {
      long value = tx.path("blockTime").asLong();
      return value > 0L ? Instant.ofEpochSecond(value) : null;
    }
    return null;
  }

  private BigDecimal parseAmount(String value) {
    try {
      return new BigDecimal(StringUtils.hasText(value) ? value.trim() : "0");
    } catch (Exception ex) {
      return BigDecimal.ZERO;
    }
  }
}
