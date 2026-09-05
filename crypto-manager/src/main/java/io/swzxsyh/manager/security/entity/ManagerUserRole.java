package io.swzxsyh.manager.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 管理端账号与角色绑定关系。 */
@Data
@TableName("manager_user_role")
public class ManagerUserRole {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private Long userId;

  private String roleCode;

  private LocalDateTime createdAt;
}
