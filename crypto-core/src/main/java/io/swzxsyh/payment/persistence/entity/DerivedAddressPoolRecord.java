package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 派生地址池记录，保存地址来源、状态与风控信息。 */
@TableName("derived_address_pool_record")
public class DerivedAddressPoolRecord {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String poolKey;
  private String chain;
  private String token;
  private String address;
  private String sourceMode;
  private String sourceReference;
  private String status;
  private String leaseId;
  private String leaseOrderNo;
  private boolean riskFlag;
  private String kytDecision;
  private String kytProvider;
  private Integer kytRiskScore;
  private String riskReason;
  private LocalDateTime generatedAt;
  private LocalDateTime leasedAt;
  private LocalDateTime releasedAt;
  private LocalDateTime lastUsedAt;
  private LocalDateTime cooldownUntil;
  private String lastOrderNo;
  private String lastTxHash;
  private Integer reuseCount;
  private LocalDateTime riskMarkedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getPoolKey() {
    return poolKey;
  }

  public void setPoolKey(String poolKey) {
    this.poolKey = poolKey;
  }

  public String getChain() {
    return chain;
  }

  public void setChain(String chain) {
    this.chain = chain;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getSourceMode() {
    return sourceMode;
  }

  public void setSourceMode(String sourceMode) {
    this.sourceMode = sourceMode;
  }

  public String getSourceReference() {
    return sourceReference;
  }

  public void setSourceReference(String sourceReference) {
    this.sourceReference = sourceReference;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getLeaseId() {
    return leaseId;
  }

  public void setLeaseId(String leaseId) {
    this.leaseId = leaseId;
  }

  public String getLeaseOrderNo() {
    return leaseOrderNo;
  }

  public void setLeaseOrderNo(String leaseOrderNo) {
    this.leaseOrderNo = leaseOrderNo;
  }

  public boolean isRiskFlag() {
    return riskFlag;
  }

  public void setRiskFlag(boolean riskFlag) {
    this.riskFlag = riskFlag;
  }

  public String getKytDecision() {
    return kytDecision;
  }

  public void setKytDecision(String kytDecision) {
    this.kytDecision = kytDecision;
  }

  public String getKytProvider() {
    return kytProvider;
  }

  public void setKytProvider(String kytProvider) {
    this.kytProvider = kytProvider;
  }

  public Integer getKytRiskScore() {
    return kytRiskScore;
  }

  public void setKytRiskScore(Integer kytRiskScore) {
    this.kytRiskScore = kytRiskScore;
  }

  public String getRiskReason() {
    return riskReason;
  }

  public void setRiskReason(String riskReason) {
    this.riskReason = riskReason;
  }

  public LocalDateTime getGeneratedAt() {
    return generatedAt;
  }

  public void setGeneratedAt(LocalDateTime generatedAt) {
    this.generatedAt = generatedAt;
  }

  public LocalDateTime getLeasedAt() {
    return leasedAt;
  }

  public void setLeasedAt(LocalDateTime leasedAt) {
    this.leasedAt = leasedAt;
  }

  public LocalDateTime getReleasedAt() {
    return releasedAt;
  }

  public void setReleasedAt(LocalDateTime releasedAt) {
    this.releasedAt = releasedAt;
  }

  public LocalDateTime getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(LocalDateTime lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }

  public LocalDateTime getCooldownUntil() {
    return cooldownUntil;
  }

  public void setCooldownUntil(LocalDateTime cooldownUntil) {
    this.cooldownUntil = cooldownUntil;
  }

  public String getLastOrderNo() {
    return lastOrderNo;
  }

  public void setLastOrderNo(String lastOrderNo) {
    this.lastOrderNo = lastOrderNo;
  }

  public String getLastTxHash() {
    return lastTxHash;
  }

  public void setLastTxHash(String lastTxHash) {
    this.lastTxHash = lastTxHash;
  }

  public Integer getReuseCount() {
    return reuseCount;
  }

  public void setReuseCount(Integer reuseCount) {
    this.reuseCount = reuseCount;
  }

  public LocalDateTime getRiskMarkedAt() {
    return riskMarkedAt;
  }

  public void setRiskMarkedAt(LocalDateTime riskMarkedAt) {
    this.riskMarkedAt = riskMarkedAt;
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
