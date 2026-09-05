package io.swzxsyh.manager.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 角色数据权限，控制能查看哪些商户、链或全局数据。 */
@Data
@TableName("manager_role_data_permission")
public class ManagerRoleDataPermission {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String roleCode;

  private String scopeType;

  private String scopeValue;

  private Boolean enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
