package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 商户主数据，只保存商户身份和基础状态，签名、费率、权限等领域能力由独立表扩展。 */
@TableName("merchant_info")
public class MerchantInfo {

  /** 主键。 */
  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  /** 商户号，业务侧稳定标识。 */
  private String merchantId;
  /** 商户名称。 */
  private String merchantName;
  /** 商户状态：ENABLED/DISABLED/REVIEW。 */
  private String status;
  /** 联系邮箱。 */
  private String contactEmail;
  /** 联系电话。 */
  private String contactPhone;
  /** 默认异步通知地址，订单未传 notifyUrl 时可作为后续扩展兜底。 */
  private String defaultNotifyUrl;
  /** 默认支付完成跳转地址，订单未传 returnUrl 时可作为后续扩展兜底。 */
  private String defaultReturnUrl;
  /** 默认提现链，提现未指定链时使用。 */
  private String defaultWithdrawChain;
  /** 默认提现收款地址，提现未指定地址时使用。 */
  private String defaultWithdrawAddress;
  /** 备注。 */
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

  public String getMerchantName() {
    return merchantName;
  }

  public void setMerchantName(String merchantName) {
    this.merchantName = merchantName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getDefaultNotifyUrl() {
    return defaultNotifyUrl;
  }

  public void setDefaultNotifyUrl(String defaultNotifyUrl) {
    this.defaultNotifyUrl = defaultNotifyUrl;
  }

  public String getDefaultReturnUrl() {
    return defaultReturnUrl;
  }

  public void setDefaultReturnUrl(String defaultReturnUrl) {
    this.defaultReturnUrl = defaultReturnUrl;
  }

  public String getDefaultWithdrawChain() {
    return defaultWithdrawChain;
  }

  public void setDefaultWithdrawChain(String defaultWithdrawChain) {
    this.defaultWithdrawChain = defaultWithdrawChain;
  }

  public String getDefaultWithdrawAddress() {
    return defaultWithdrawAddress;
  }

  public void setDefaultWithdrawAddress(String defaultWithdrawAddress) {
    this.defaultWithdrawAddress = defaultWithdrawAddress;
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
