package io.swzxsyh.manager.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 角色功能权限，控制能访问哪些后台功能。 */
@Data
@TableName("manager_role_function_permission")
public class ManagerRoleFunctionPermission {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String roleCode;

  private String permissionCode;

  private String permissionName;

  private Boolean enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
