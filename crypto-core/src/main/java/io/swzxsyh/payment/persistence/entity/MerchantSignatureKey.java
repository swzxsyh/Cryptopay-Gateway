package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 商户验签公钥。 */
@TableName("merchant_signature_key")
public class MerchantSignatureKey {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 商户标识。 */
  private String merchantId;
  /** 密钥版本。 */
  private String keyVersion;
  /** 算法。 */
  private String algorithm;
  /** 公钥 PEM。 */
  private String publicKeyPem;
  /** 是否启用。 */
  private boolean enabled;
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

  public String getMerchantId() {
    return merchantId;
  }

  public void setMerchantId(String merchantId) {
    this.merchantId = merchantId;
  }

  public String getKeyVersion() {
    return keyVersion;
  }

  public void setKeyVersion(String keyVersion) {
    this.keyVersion = keyVersion;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public String getPublicKeyPem() {
    return publicKeyPem;
  }

  public void setPublicKeyPem(String publicKeyPem) {
    this.publicKeyPem = publicKeyPem;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
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
