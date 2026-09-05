package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 回调投递记录，用于重试、死信和追踪。 */
@TableName("payment_callback_delivery_record")
public class PaymentCallbackDeliveryRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 事件类型。 */
  private String eventType;
  /** 链名。 */
  private String chain;
  /** 平台支付单号。 */
  private String cryptoOrderNo;
  /** 商户订单号。 */
  private String merchantOrderNo;
  /** 商户标识。 */
  private String merchantId;
  /** 回调地址。 */
  private String callbackUrl;
  /** 回调事件幂等键，用于避免同一业务事件重复入队。 */
  private String callbackKey;
  /** 回调负载 JSON。 */
  private String payloadJson;
  /** 请求头 JSON。 */
  private String requestHeadersJson;
  /** 当前状态。 */
  private String status;
  /** 已重试次数。 */
  private Integer attemptCount;
  /** 最大重试次数。 */
  private Integer maxAttempts;
  /** 下次重试时间。 */
  private LocalDateTime nextRetryAt;
  /** 最近一次投递时间。 */
  private LocalDateTime lastAttemptAt;
  /** 投递成功时间。 */
  private LocalDateTime deliveredAt;
  /** 死信时间。 */
  private LocalDateTime deadAt;
  /** 最近 HTTP 状态码。 */
  private Integer lastHttpStatus;
  /** 最近响应正文。 */
  private String lastResponseBody;
  /** 最近错误信息。 */
  private String lastError;
  /** 创建时间。 */
  private LocalDateTime createdAt;
  /** 更新时间。 */
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getChain() {
    return chain;
  }

  public void setChain(String chain) {
    this.chain = chain;
  }

  public String getCryptoOrderNo() {
    return cryptoOrderNo;
  }

  public void setCryptoOrderNo(String cryptoOrderNo) {
    this.cryptoOrderNo = cryptoOrderNo;
  }

  public String getMerchantOrderNo() {
    return merchantOrderNo;
  }

  public void setMerchantOrderNo(String merchantOrderNo) {
    this.merchantOrderNo = merchantOrderNo;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getCallbackUrl() {
    return callbackUrl;
  }

  public void setCallbackUrl(String callbackUrl) {
    this.callbackUrl = callbackUrl;
  }

  public String getCallbackKey() {
    return callbackKey;
  }

  public void setCallbackKey(String callbackKey) {
    this.callbackKey = callbackKey;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(String payloadJson) {
    this.payloadJson = payloadJson;
  }

  public String getRequestHeadersJson() {
    return requestHeadersJson;
  }

  public void setRequestHeadersJson(String requestHeadersJson) {
    this.requestHeadersJson = requestHeadersJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(Integer attemptCount) {
    this.attemptCount = attemptCount;
  }

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public LocalDateTime getNextRetryAt() {
    return nextRetryAt;
  }

  public void setNextRetryAt(LocalDateTime nextRetryAt) {
    this.nextRetryAt = nextRetryAt;
  }

  public LocalDateTime getLastAttemptAt() {
    return lastAttemptAt;
  }

  public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
    this.lastAttemptAt = lastAttemptAt;
  }

  public LocalDateTime getDeliveredAt() {
    return deliveredAt;
  }

  public void setDeliveredAt(LocalDateTime deliveredAt) {
    this.deliveredAt = deliveredAt;
  }

  public LocalDateTime getDeadAt() {
    return deadAt;
  }

  public void setDeadAt(LocalDateTime deadAt) {
    this.deadAt = deadAt;
  }

  public Integer getLastHttpStatus() {
    return lastHttpStatus;
  }

  public void setLastHttpStatus(Integer lastHttpStatus) {
    this.lastHttpStatus = lastHttpStatus;
  }

  public String getLastResponseBody() {
    return lastResponseBody;
  }

  public void setLastResponseBody(String lastResponseBody) {
    this.lastResponseBody = lastResponseBody;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
