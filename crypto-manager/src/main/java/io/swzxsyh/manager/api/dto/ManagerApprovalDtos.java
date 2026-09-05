package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.ManagerApprovalAction;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalDefinition;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalStepDefinition;
import java.time.LocalDateTime;
import java.util.List;

/** 管理端通用多人审批 DTO。 */
public final class ManagerApprovalDtos {

  private ManagerApprovalDtos() {}

  public record ApprovalActionRequest(String comment, String rejectReason) {}

  public record ApprovalDefinitionSaveRequest(
      Long id,
      String definitionCode,
      String bizType,
      String definitionName,
      Boolean notifyNextApprover,
      Boolean enabled,
      List<ApprovalStepSaveRequest> steps) {}

  public record ApprovalOption(String label, String value) {}

  public record ApprovalOptionsResponse(
      List<ApprovalOption> bizTypes,
      List<ApprovalOption> approverUsers,
      List<ApprovalOption> approverRoles) {}

  public record ApprovalStepSaveRequest(
      Long id,
      String definitionCode,
      Integer stepNo,
      String stepName,
      Integer requiredApprovals,
      String approverUsers,
      String approverRoles,
      Boolean enabled) {}

  public record ApprovalDefinitionView(
      Long id,
      String definitionCode,
      String bizType,
      String definitionName,
      Boolean notifyNextApprover,
      Boolean enabled,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static ApprovalDefinitionView from(ManagerApprovalDefinition definition) {
      return new ApprovalDefinitionView(
          definition.getId(),
          definition.getDefinitionCode(),
          definition.getBizType(),
          definition.getDefinitionName(),
          definition.getNotifyNextApprover(),
          definition.getEnabled(),
          definition.getCreatedAt(),
          definition.getUpdatedAt());
    }
  }

  public record ApprovalStepView(
      Long id,
      String definitionCode,
      Integer stepNo,
      String stepName,
      Integer requiredApprovals,
      String approverUsers,
      String approverRoles,
      Boolean enabled,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static ApprovalStepView from(ManagerApprovalStepDefinition step) {
      return new ApprovalStepView(
          step.getId(),
          step.getDefinitionCode(),
          step.getStepNo(),
          step.getStepName(),
          step.getRequiredApprovals(),
          step.getApproverUsers(),
          step.getApproverRoles(),
          step.getEnabled(),
          step.getCreatedAt(),
          step.getUpdatedAt());
    }
  }

  public record ApprovalRequestView(
      Long id,
      String approvalNo,
      String bizType,
      String bizKey,
      String definitionCode,
      String title,
      String status,
      Integer currentStepNo,
      Integer totalSteps,
      Integer requiredApprovals,
      Integer approvedCount,
      String applicant,
      String payloadJson,
      String remark,
      String rejectReason,
      LocalDateTime completedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static ApprovalRequestView from(ManagerApprovalRequest request) {
      return new ApprovalRequestView(
          request.getId(),
          request.getApprovalNo(),
          request.getBizType(),
          request.getBizKey(),
          request.getDefinitionCode(),
          request.getTitle(),
          request.getStatus(),
          request.getCurrentStepNo(),
          request.getTotalSteps(),
          request.getRequiredApprovals(),
          request.getApprovedCount(),
          request.getApplicant(),
          request.getPayloadJson(),
          request.getRemark(),
          request.getRejectReason(),
          request.getCompletedAt(),
          request.getCreatedAt(),
          request.getUpdatedAt());
    }
  }

  public record ApprovalActionView(
      Long id,
      String approvalNo,
      Integer stepNo,
      String approver,
      String action,
      String comment,
      LocalDateTime createdAt) {

    public static ApprovalActionView from(ManagerApprovalAction action) {
      return new ApprovalActionView(
          action.getId(),
          action.getApprovalNo(),
          action.getStepNo(),
          action.getApprover(),
          action.getAction(),
          action.getComment(),
          action.getCreatedAt());
    }
  }

  public record ApprovalDetailResponse(
      ApprovalRequestView request,
      List<ApprovalActionView> actions) {}
}
