package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 通用多人审批动作记录，保证同一审批单同一审批人只能操作一次。 */
@Data
@TableName("manager_approval_action")
public class ManagerApprovalAction {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String approvalNo;
  private Integer stepNo;
  private String approver;
  private String action;
  private String comment;
  private LocalDateTime createdAt;
}
