package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 平台签名密钥。 */
@TableName("platform_signing_key")
public class PlatformSigningKey {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 密钥别名。 */
  private String keyAlias;
  /** 算法。 */
  private String algorithm;
  /** 私钥 PEM。 */
  private String privateKeyPem;
  /** 公钥 PEM。 */
  private String publicKeyPem;
  /** 是否启用。 */
  private boolean active;
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

  public String getKeyAlias() {
    return keyAlias;
  }

  public void setKeyAlias(String keyAlias) {
    this.keyAlias = keyAlias;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public void setAlgorithm(String algorithm) {
    this.algorithm = algorithm;
  }

  public String getPrivateKeyPem() {
    return privateKeyPem;
  }

  public void setPrivateKeyPem(String privateKeyPem) {
    this.privateKeyPem = privateKeyPem;
  }

  public String getPublicKeyPem() {
    return publicKeyPem;
  }

  public void setPublicKeyPem(String publicKeyPem) {
    this.publicKeyPem = publicKeyPem;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
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
