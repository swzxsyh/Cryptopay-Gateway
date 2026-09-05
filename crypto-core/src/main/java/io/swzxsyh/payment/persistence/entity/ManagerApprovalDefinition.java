package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 审批流定义，按业务类型配置是否启用、默认需要几步审批。 */
@Data
@TableName("manager_approval_definition")
public class ManagerApprovalDefinition {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String definitionCode;
  private String bizType;
  private String definitionName;
  /** 是否在审批单提交或流转到下一步骤时通知下一责任人，默认关闭。 */
  private Boolean notifyNextApprover;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
