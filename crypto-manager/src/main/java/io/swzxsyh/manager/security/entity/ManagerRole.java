package io.swzxsyh.manager.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 管理端角色定义，ADMIN 或 super_admin 角色拥有全部权限。 */
@Data
@TableName("manager_role")
public class ManagerRole {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String roleCode;

  private String roleName;

  private Boolean superAdmin;

  private Boolean enabled;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
