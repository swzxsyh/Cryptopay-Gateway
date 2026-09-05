package io.swzxsyh.payment.callback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.alert.PaymentAlertService;
import io.swzxsyh.payment.config.CryptoPaymentProperties;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import io.swzxsyh.payment.signature.PlatformCallbackSignatureService;
import io.swzxsyh.payment.util.HttpClientUtil;
import io.swzxsyh.payment.util.HttpClientUtil.ExternalHttpResponse;
import io.swzxsyh.payment.util.LockUtil;
import io.swzxsyh.payment.util.JsonUtil;
import io.swzxsyh.payment.util.RedisKeyNamespace;
import io.swzxsyh.watcher.dto.PaymentNotificationRecord;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 回调投递、重试和死信管理服务。 */
@Slf4j
@Service
public class PaymentCallbackDeliveryService {

  private final PaymentCallbackDeliveryRecordMapper recordMapper;
  private final CryptoPaymentProperties properties;
  private final PlatformCallbackSignatureService signatureService;
  private final PaymentAuditService auditService;
  private final PaymentAlertService alertService;
  private final LockUtil lockUtil;
  private final Executor callbackTaskExecutor;
  private final HttpClientUtil httpClientUtil;

  public PaymentCallbackDeliveryService(
      PaymentCallbackDeliveryRecordMapper recordMapper,
      CryptoPaymentProperties properties,
      PlatformCallbackSignatureService signatureService,
      PaymentAuditService auditService,
      PaymentAlertService alertService,
      LockUtil lockUtil,
      @Qualifier("callbackTaskExecutor") Executor callbackTaskExecutor,
      HttpClientUtil httpClientUtil) {
    this.recordMapper = recordMapper;
    this.properties = properties;
    this.signatureService = signatureService;
    this.auditService = auditService;
    this.alertService = alertService;
    this.lockUtil = lockUtil;
    this.callbackTaskExecutor = callbackTaskExecutor;
    this.httpClientUtil = httpClientUtil;
  }

  /** 入队一条回调任务。 */
  public PaymentCallbackDeliveryRecord enqueue(
      String eventType,
      String merchantId,
      String callbackUrl,
      PaymentNotificationRecord notification) {
    return enqueuePayload(
        eventType,
        merchantId,
        notification.chain(),
        notification.cryptoOrderNo(),
        notification.merchantOrderNo(),
        callbackUrl,
        notification);
  }

  /** 入队一条通用回调任务，普通订单和订阅账单都走这条底层链路。 */
  public PaymentCallbackDeliveryRecord enqueuePayload(
      String eventType,
      String merchantId,
      String chain,
      String bizOrderNo,
      String merchantOrderNo,
      String callbackUrl,
      Object payload) {
    String payloadJson = writeJson(payload);
    String canonicalPayloadJson = JsonUtil.toCanonicalJson(payload);
    String callbackKey =
        buildCallbackKey(eventType, merchantId, chain, bizOrderNo, merchantOrderNo, canonicalPayloadJson);

    PaymentCallbackDeliveryRecord existing = findByCallbackKey(callbackKey).orElse(null);
    if (existing != null) {
      log.info(
          "Callback delivery already queued. eventType={}, bizOrderNo={}, callbackKey={}, recordId={}, status={}",
          eventType,
          bizOrderNo,
          callbackKey,
          existing.getId(),
          existing.getStatus());
      return existing;
    }

    PaymentCallbackDeliveryRecord record = new PaymentCallbackDeliveryRecord();
    LocalDateTime now = LocalDateTime.now();
    record.setEventType(eventType);
    record.setChain(chain);
    record.setCryptoOrderNo(bizOrderNo);
    record.setMerchantOrderNo(merchantOrderNo);
    record.setMerchantId(merchantId);
    record.setCallbackUrl(callbackUrl);
    record.setCallbackKey(callbackKey);
    record.setPayloadJson(payloadJson);
    record.setRequestHeadersJson(writeJson(Map.of()));
    record.setStatus(CallbackDeliveryStatus.PENDING.name());
    record.setAttemptCount(0);
    record.setMaxAttempts(Math.max(1, properties.getCallback().getMaxAttempts()));
    record.setNextRetryAt(now);
    record.setCreatedAt(now);
    record.setUpdatedAt(now);
    try {
      recordMapper.insert(record);
    } catch (DuplicateKeyException ex) {
      PaymentCallbackDeliveryRecord duplicated =
          findByCallbackKey(callbackKey)
              .orElseThrow(() -> new IllegalStateException("Duplicate callback key but record not found", ex));
      log.info(
          "Callback delivery duplicate insert ignored. eventType={}, bizOrderNo={}, callbackKey={}, recordId={}",
          eventType,
          bizOrderNo,
          callbackKey,
          duplicated.getId());
      return duplicated;
    }
    auditService.record("CALLBACK_ENQUEUED", eventType, bizOrderNo, "PENDING", record);
    log.info(
        "Callback delivery queued. eventType={}, cryptoOrderNo={}, merchantOrderNo={}, callbackUrl={}",
        eventType,
        bizOrderNo,
        merchantOrderNo,
        callbackUrl);
    return record;
  }

  /** 入队后立即尝试投递。 */
  @Async("callbackTaskExecutor")
  public void enqueueAndDispatch(
      String eventType,
      String merchantId,
      String callbackUrl,
      PaymentNotificationRecord notification) {
    if (!StringUtils.hasText(callbackUrl)) {
      log.warn(
          "Skip callback because callbackUrl is blank. eventType={}, cryptoOrderNo={}",
          eventType,
          notification.cryptoOrderNo());
      return;
    }
    PaymentCallbackDeliveryRecord record =
        enqueue(eventType, merchantId, callbackUrl, notification);
    if (!properties.getCallback().isDispatchImmediately()) {
      log.info(
          "Callback delivery queued for delayed dispatch. eventType={}, cryptoOrderNo={}, recordId={}",
          eventType,
          notification.cryptoOrderNo(),
          record.getId());
      return;
    }
    dispatchRecord(record.getId());
  }

  /** 入队通用回调负载后立即尝试投递。 */
  @Async("callbackTaskExecutor")
  public void enqueuePayloadAndDispatch(
      String eventType,
      String merchantId,
      String chain,
      String bizOrderNo,
      String merchantOrderNo,
      String callbackUrl,
      Object payload) {
    if (!StringUtils.hasText(callbackUrl)) {
      log.warn(
          "Skip callback because callbackUrl is blank. eventType={}, bizOrderNo={}",
          eventType,
          bizOrderNo);
      return;
    }
    PaymentCallbackDeliveryRecord record =
        enqueuePayload(eventType, merchantId, chain, bizOrderNo, merchantOrderNo, callbackUrl, payload);
    if (!properties.getCallback().isDispatchImmediately()) {
      log.info(
          "Callback delivery queued for delayed dispatch. eventType={}, bizOrderNo={}, recordId={}",
          eventType,
          bizOrderNo,
          record.getId());
      return;
    }
    dispatchRecord(record.getId());
  }

  /** 扫描并投递到期的回调任务。 */
  @Scheduled(fixedDelayString = "#{T(java.lang.Math).max(1000L, @cryptoPaymentProperties.callback.initialBackoffSeconds * 1000L)}")
  public void dispatchDueCallbacks() {
    if (!properties.getCallback().isRetryEnabled()) {
      return;
    }

    List<PaymentCallbackDeliveryRecord> dueRecords =
        recordMapper.selectList(
            new LambdaQueryWrapper<PaymentCallbackDeliveryRecord>()
                .in(
                    PaymentCallbackDeliveryRecord::getStatus,
                    CallbackDeliveryStatus.PENDING.name(),
                    CallbackDeliveryStatus.RETRYING.name())
                .le(PaymentCallbackDeliveryRecord::getNextRetryAt, LocalDateTime.now())
                .orderByAsc(PaymentCallbackDeliveryRecord::getId)
                .last("LIMIT " + callbackDispatchBatchSize()));

    for (PaymentCallbackDeliveryRecord record : dueRecords) {
      callbackTaskExecutor.execute(() -> dispatchRecord(record.getId()));
    }
  }

  /** 清理过期死信记录。 */
  @Scheduled(cron = "0 20 3 * * ?")
  public void purgeDeadLetters() {
    if (!properties.getCallback().isDeadLetterEnabled()) {
      return;
    }

    LocalDateTime threshold =
        LocalDateTime.now().minusDays(Math.max(1L, properties.getCallback().getRetentionDays()));
    int removed =
        recordMapper.delete(
            new LambdaQueryWrapper<PaymentCallbackDeliveryRecord>()
                .eq(PaymentCallbackDeliveryRecord::getStatus, CallbackDeliveryStatus.DEAD.name())
                .lt(PaymentCallbackDeliveryRecord::getUpdatedAt, threshold));
    if (removed > 0) {
      log.info("Purged dead callback records. removed={}, threshold={}", removed, threshold);
    }
  }

  /** 根据 ID 查询回调记录。 */
  public Optional<PaymentCallbackDeliveryRecord> findById(Long id) {
    return Optional.ofNullable(recordMapper.selectById(id));
  }

  /** 根据事件幂等键查询回调记录。 */
  public Optional<PaymentCallbackDeliveryRecord> findByCallbackKey(String callbackKey) {
    if (!StringUtils.hasText(callbackKey)) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        recordMapper.selectOne(
            new LambdaQueryWrapper<PaymentCallbackDeliveryRecord>()
                .eq(PaymentCallbackDeliveryRecord::getCallbackKey, callbackKey)));
  }

  /** 手动重放某条回调。 */
  public void replay(Long recordId) {
    dispatchRecord(recordId);
  }

  private void dispatchRecord(Long recordId) {
    if (recordId == null) {
      return;
    }

    String lockKey = RedisKeyNamespace.callbackDispatchLock(properties, recordId);
    try {
      lockUtil.withLock(
          lockKey,
          callbackDispatchLockWaitMillis(),
          callbackDispatchLockLeaseSeconds(),
          () -> {
            PaymentCallbackDeliveryRecord record = recordMapper.selectById(recordId);
            if (record == null) {
              return null;
            }
            if (CallbackDeliveryStatus.DELIVERED.name().equals(record.getStatus())
                || CallbackDeliveryStatus.DEAD.name().equals(record.getStatus())) {
              return null;
            }
            attemptDelivery(record);
            return null;
          });
    } catch (Exception ex) {
      log.warn(
          "Callback dispatch skipped or failed before send. recordId={}, error={}",
          recordId,
          ex.getMessage());
    }
  }

  private void attemptDelivery(PaymentCallbackDeliveryRecord record) {
    int attempt = record.getAttemptCount() == null ? 0 : record.getAttemptCount();
    int nextAttempt = attempt + 1;
    LocalDateTime now = LocalDateTime.now();

    record.setAttemptCount(nextAttempt);
    record.setLastAttemptAt(now);
    record.setStatus(
        nextAttempt == 1
            ? CallbackDeliveryStatus.PENDING.name()
            : CallbackDeliveryStatus.RETRYING.name());
    record.setUpdatedAt(now);

    HttpEntity<String> requestEntity = buildRequestEntity(record);
    try {
      ExternalHttpResponse response =
          httpClientUtil.postJson(
              record.getCallbackUrl(),
              requestEntity.getBody(),
              requestEntity.getHeaders().toSingleValueMap());
      int statusCode = response.statusCode();
      record.setLastHttpStatus(statusCode);
      record.setLastResponseBody(truncate(response.body(), maxStoredResponseChars()));
      if (response.is2xxSuccessful()) {
        record.setStatus(CallbackDeliveryStatus.DELIVERED.name());
        record.setDeliveredAt(now);
        record.setNextRetryAt(null);
        record.setLastError(null);
        record.setUpdatedAt(now);
        recordMapper.updateById(record);
        auditService.record(
            "CALLBACK_DELIVERED",
            record.getEventType(),
            record.getCryptoOrderNo(),
            "DELIVERED",
            record);
        log.info(
            "Callback delivered. cryptoOrderNo={}, merchantOrderNo={}, attempt={}, httpStatus={}",
            record.getCryptoOrderNo(),
            record.getMerchantOrderNo(),
            nextAttempt,
            statusCode);
        return;
      }

      throw new IllegalStateException("callback returned non-2xx status: " + statusCode);
    } catch (Exception ex) {
      failRecord(record, nextAttempt, ex.getMessage());
    }
  }

  private HttpEntity<String> buildRequestEntity(
      PaymentCallbackDeliveryRecord record) {
    String body = record.getPayloadJson();
    HttpEntity<String> signed =
        signatureService.signJson(record.getMerchantId(), body);
    HttpHeaders headers = new HttpHeaders();
    headers.putAll(signed.getHeaders());
    headers.add("X-Callback-Event", record.getEventType());
    headers.add(
        "X-Callback-Attempt",
        String.valueOf(record.getAttemptCount() == null ? 0 : record.getAttemptCount()));
    record.setRequestHeadersJson(writeJson(headers.toSingleValueMap()));
    return new HttpEntity<>(signed.getBody(), headers);
  }

  private void failRecord(PaymentCallbackDeliveryRecord record, int attempt, String error) {
    LocalDateTime now = LocalDateTime.now();
    int maxAttempts =
        Math.max(
            1,
            record.getMaxAttempts() == null
                ? properties.getCallback().getMaxAttempts()
                : record.getMaxAttempts());
    record.setLastError(truncate(error, maxStoredErrorChars()));
    record.setUpdatedAt(now);

    if (attempt >= maxAttempts) {
      record.setStatus(CallbackDeliveryStatus.DEAD.name());
      record.setDeadAt(now);
      record.setNextRetryAt(null);
      auditService.record(
          "CALLBACK_DEAD", record.getEventType(), record.getCryptoOrderNo(), "DEAD", record);
      alertService.alertCallbackDeadLetter(
          record.getCryptoOrderNo(),
          record.getMerchantOrderNo(),
          record.getCallbackUrl(),
          error);
      log.error(
          "Callback dead-lettered. cryptoOrderNo={}, merchantOrderNo={}, attempts={}, error={}",
          record.getCryptoOrderNo(),
          record.getMerchantOrderNo(),
          attempt,
          error);
    } else {
      long delaySeconds = computeBackoffSeconds(attempt);
      record.setStatus(CallbackDeliveryStatus.RETRYING.name());
      record.setNextRetryAt(now.plusSeconds(delaySeconds));
      auditService.record(
          "CALLBACK_RETRYING",
          record.getEventType(),
          record.getCryptoOrderNo(),
          "RETRYING",
          record);
      log.warn(
          "Callback delivery failed. cryptoOrderNo={}, merchantOrderNo={}, attempt={}, nextRetryAt={}, error={}",
          record.getCryptoOrderNo(),
          record.getMerchantOrderNo(),
          attempt,
          record.getNextRetryAt(),
          error);
    }
    recordMapper.updateById(record);
  }

  private long computeBackoffSeconds(int attempt) {
    long initial = Math.max(1L, properties.getCallback().getInitialBackoffSeconds());
    long max = Math.max(initial, properties.getCallback().getMaxBackoffSeconds());
    long backoff = initial * (1L << Math.max(0, attempt - 1));
    return Math.min(backoff, max);
  }

  private String writeJson(Object value) {
    return JsonUtil.toJson(value);
  }

  private String buildCallbackKey(
      String eventType,
      String merchantId,
      String chain,
      String bizOrderNo,
      String merchantOrderNo,
      String payloadJson) {
    String canonical =
        normalize(eventType)
            + "\n"
            + normalize(merchantId)
            + "\n"
            + normalize(chain)
            + "\n"
            + normalize(bizOrderNo)
            + "\n"
            + normalize(merchantOrderNo)
            + "\n"
            + (payloadJson == null ? "" : payloadJson);
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to build callback key", ex);
    }
  }

  private String normalize(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", "_").toLowerCase();
  }

  private int callbackDispatchBatchSize() {
    return Math.max(1, properties.getCallback().getDispatchBatchSize());
  }

  private long callbackDispatchLockWaitMillis() {
    return Math.max(100L, properties.getCallback().getDispatchLockWaitMillis());
  }

  private long callbackDispatchLockLeaseSeconds() {
    long callTimeoutMillis = Math.max(1L, properties.getExternalHttp().getCallTimeoutMillis());
    long callTimeoutSeconds = Math.max(1L, (callTimeoutMillis + 999L) / 1000L);
    return Math.max(callTimeoutSeconds + 5L, properties.getCallback().getDispatchLockLeaseSeconds());
  }

  private int maxStoredResponseChars() {
    return Math.max(1000, properties.getCallback().getMaxStoredResponseChars());
  }

  private int maxStoredErrorChars() {
    return Math.max(1000, properties.getCallback().getMaxStoredErrorChars());
  }

  private String truncate(String value, int maxChars) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.length() <= maxChars ? value : value.substring(0, maxChars);
  }
}
