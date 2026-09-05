package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 商户产品库记录，用于管理端登记商户的产品入口、测试账号和内部备注。 */
@TableName("merchant_product")
public class MerchantProduct {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 商户号。 */
  private String merchantId;
  /** 产品名。 */
  private String productName;
  /** 产品访问链接。 */
  private String productUrl;
  /** 测试账号。 */
  private String testUsername;
  /** 测试密码。 */
  private String testPassword;
  /** 内部备注。 */
  private String remark;
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

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductUrl() {
    return productUrl;
  }

  public void setProductUrl(String productUrl) {
    this.productUrl = productUrl;
  }

  public String getTestUsername() {
    return testUsername;
  }

  public void setTestUsername(String testUsername) {
    this.testUsername = testUsername;
  }

  public String getTestPassword() {
    return testPassword;
  }

  public void setTestPassword(String testPassword) {
    this.testPassword = testPassword;
  }

  public String getRemark() {
    return remark;
  }

  public void setRemark(String remark) {
    this.remark = remark;
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
