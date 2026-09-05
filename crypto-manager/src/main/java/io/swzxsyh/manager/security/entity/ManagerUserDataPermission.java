package io.swzxsyh.manager.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 用户数据权限，主要用于给管理端人员单独绑定可访问商户。 */
@Data
@TableName("manager_user_data_permission")
public class ManagerUserDataPermission {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Long userId;

  private String scopeType;

  private String scopeValue;

  private Boolean enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
