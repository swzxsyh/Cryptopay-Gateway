package io.swzxsyh.payment.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 通用多人审批申请，使用 bizType + bizKey 绑定具体业务。 */
@Data
@TableName("manager_approval_request")
public class ManagerApprovalRequest {

  @TableId(value = "id", type = IdType.AUTO)
  private Long id;
  private String approvalNo;
  private String bizType;
  private String bizKey;
  private String definitionCode;
  private String title;
  private String status;
  private Integer currentStepNo;
  private Integer totalSteps;
  private Integer requiredApprovals;
  private Integer approvedCount;
  private String applicant;
  private String payloadJson;
  private String remark;
  private String rejectReason;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
