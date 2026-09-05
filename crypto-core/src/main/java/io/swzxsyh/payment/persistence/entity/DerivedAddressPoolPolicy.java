package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** 派生地址池策略配置，记录最小池大小等库存参数。 */
@TableName("derived_address_pool_policy")
public class DerivedAddressPoolPolicy {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String poolKey;
  private Integer minSize;
  private Boolean enabled;
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

  public Integer getMinSize() {
    return minSize;
  }

  public void setMinSize(Integer minSize) {
    this.minSize = minSize;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
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
