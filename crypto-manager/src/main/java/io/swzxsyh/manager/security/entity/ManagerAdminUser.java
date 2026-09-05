package io.swzxsyh.manager.security.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 管理端管理员账号。 */
@Data
@TableName("manager_admin_user")
public class ManagerAdminUser {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;

  private String username;

  private String passwordHash;

  private String displayName;

  /** 联系方式类型：PHONE/EMAIL，后续审批通知可按类型路由短信或邮件。 */
  private String contactType;

  /** 联系方式内容：手机号或邮箱地址。 */
  private String contactValue;

  private Boolean enabled;

  private Boolean accountNonLocked;

  private Integer failedLoginCount;

  private LocalDateTime lastLoginAt;

  private String roles;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
