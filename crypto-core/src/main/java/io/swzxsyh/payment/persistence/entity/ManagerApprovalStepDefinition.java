package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 审批步骤定义，每一步可配置审批人数、审批用户白名单和审批角色白名单。 */
@Data
@TableName("manager_approval_step_definition")
public class ManagerApprovalStepDefinition {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String definitionCode;
  private Integer stepNo;
  private String stepName;
  private Integer requiredApprovals;
  private String approverUsers;
  private String approverRoles;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
