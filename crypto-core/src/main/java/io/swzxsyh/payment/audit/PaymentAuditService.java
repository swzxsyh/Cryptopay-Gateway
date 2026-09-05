package io.swzxsyh.payment.audit;

import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.util.JsonUtil;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 审计记录服务，用于记录关键订单和流程事件。 */
@Slf4j
@Service
public class PaymentAuditService {

  private static final String DETAIL_SCHEMA_VERSION = "1";

  private final PaymentAuditRecordMapper recordMapper;

  public PaymentAuditService(PaymentAuditRecordMapper recordMapper) {
    this.recordMapper = recordMapper;
  }

  /** 写入一条审计记录。 */
  public void record(
      String eventType, String bizType, String bizKey, String status, Object detail) {
    PaymentAuditRecord record = new PaymentAuditRecord();
    record.setEventType(eventType);
    record.setBizType(bizType);
    record.setBizKey(bizKey);
    record.setMerchantId(resolveMerchantId(detail));
    record.setDetailSchemaVersion(DETAIL_SCHEMA_VERSION);
    record.setStatus(status);
    record.setDetailJson(writeJson(detail));
    record.setCreatedAt(LocalDateTime.now());
    recordMapper.insert(record);
    log.info(
        "Audit recorded. eventType={}, bizType={}, bizKey={}, status={}",
        eventType,
        bizType,
        bizKey,
        status);
  }

  /** 将明细对象序列化为 JSON，失败时返回可落库的降级文本。 */
  private String writeJson(Object value) {
    try {
      var envelope = JsonUtil.createObjectNode();
      envelope.put("schemaVersion", DETAIL_SCHEMA_VERSION);
      envelope.put("capturedAt", LocalDateTime.now().toString());
      envelope.set("payload", value == null ? JsonUtil.createObjectNode() : JsonUtil.toTree(value));
      return JsonUtil.toJson(envelope);
    } catch (RuntimeException e) {
      String message = e.getMessage() == null ? "unknown" : e.getMessage().replace("\"", "'");
      return "{\"serializationError\":\"" + message + "\"}";
    }
  }

  private String resolveMerchantId(Object detail) {
    if (detail == null) {
      return null;
    }
    try {
      var tree = JsonUtil.toTree(detail);
      if (tree != null && tree.hasNonNull("merchantId")) {
        String merchantId = tree.get("merchantId").asText();
        return merchantId == null || merchantId.isBlank() ? null : merchantId;
      }
    } catch (Exception ignored) {
      // ignore and fall through
    }
    return null;
  }
}
