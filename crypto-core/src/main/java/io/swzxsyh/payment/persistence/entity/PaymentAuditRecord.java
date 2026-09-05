package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 订单与流程审计记录。 */
@TableName("payment_audit_record")
public class PaymentAuditRecord {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 事件类型。 */
  private String eventType;
  /** 业务类型。 */
  private String bizType;
  /** 业务标识。 */
  private String bizKey;
  /** 商户号。 */
  private String merchantId;
  /** 明细结构版本。 */
  private String detailSchemaVersion;
  /** 业务状态。 */
  private String status;
  /** 事件详情 JSON。 */
  private String detailJson;
  /** 创建时间。 */
  private LocalDateTime createdAt;

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

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public String getBizKey() {
    return bizKey;
  }

  public void setBizKey(String bizKey) {
    this.bizKey = bizKey;
  }

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getDetailSchemaVersion() {
    return detailSchemaVersion;
  }

  public void setDetailSchemaVersion(String detailSchemaVersion) {
    this.detailSchemaVersion = detailSchemaVersion;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDetailJson() {
    return detailJson;
  }

  public void setDetailJson(String detailJson) {
    this.detailJson = detailJson;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
