package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalActionRequest;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalActionView;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalDefinitionSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalDefinitionView;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalOption;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalOptionsResponse;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalRequestView;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalStepSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerApprovalDtos.ApprovalStepView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.approval.ApprovalBusinessHandler;
import io.swzxsyh.manager.security.ManagerPrincipal;
import io.swzxsyh.manager.security.ManagerSecurityContext;
import io.swzxsyh.manager.security.entity.ManagerAdminUser;
import io.swzxsyh.manager.security.entity.ManagerRole;
import io.swzxsyh.manager.security.entity.ManagerUserRole;
import io.swzxsyh.manager.security.mapper.ManagerAdminUserMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserRoleMapper;
import io.swzxsyh.manager.message.ManagerMessageApplicationService;
import io.swzxsyh.payment.audit.PaymentAuditService;
import io.swzxsyh.payment.mapper.ManagerApprovalActionMapper;
import io.swzxsyh.payment.mapper.ManagerApprovalDefinitionMapper;
import io.swzxsyh.payment.mapper.ManagerApprovalRequestMapper;
import io.swzxsyh.payment.mapper.ManagerApprovalStepDefinitionMapper;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalAction;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalDefinition;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalStepDefinition;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 通用多人审批应用服务，审批通过后通过 handler 分发到具体业务。 */
@Service
public class ManagerApprovalApplicationService extends ManagerApplicationSupport {

  private static final int MIN_REQUIRED_APPROVALS = 2;
  private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final Logger log = LoggerFactory.getLogger(ManagerApprovalApplicationService.class);

  private final ManagerApprovalRequestMapper requestMapper;
  private final ManagerApprovalActionMapper actionMapper;
  private final ManagerApprovalDefinitionMapper definitionMapper;
  private final ManagerApprovalStepDefinitionMapper stepMapper;
  private final ManagerAdminUserMapper adminUserMapper;
  private final ManagerRoleMapper roleMapper;
  private final ManagerUserRoleMapper userRoleMapper;
  private final ManagerMessageApplicationService messageService;
  private final PaymentAuditService auditService;
  private final Map<String, ApprovalBusinessHandler> handlers;

  public ManagerApprovalApplicationService(
      ManagerApprovalRequestMapper requestMapper,
      ManagerApprovalActionMapper actionMapper,
      ManagerApprovalDefinitionMapper definitionMapper,
      ManagerApprovalStepDefinitionMapper stepMapper,
      ManagerAdminUserMapper adminUserMapper,
      ManagerRoleMapper roleMapper,
      ManagerUserRoleMapper userRoleMapper,
      ManagerMessageApplicationService messageService,
      PaymentAuditService auditService,
      List<ApprovalBusinessHandler> handlers) {
    this.requestMapper = requestMapper;
    this.actionMapper = actionMapper;
    this.definitionMapper = definitionMapper;
    this.stepMapper = stepMapper;
    this.adminUserMapper = adminUserMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.messageService = messageService;
    this.auditService = auditService;
    this.handlers = handlers.stream().collect(Collectors.toMap(
        handler -> handler.bizType().toUpperCase(),
        Function.identity(),
        (left, right) -> left));
  }

  /** 提交一个待审批业务申请。 */
  @Transactional(rollbackFor = Exception.class)
  public ManagerApprovalRequest submit(
      String bizType, String bizKey, String title, String payloadJson, Integer requiredApprovals) {
    return submit(bizType, bizKey, title, payloadJson, requiredApprovals, null);
  }

  /** 提交一个待审批业务申请，可附带业务备注。 */
  @Transactional(rollbackFor = Exception.class)
  public ManagerApprovalRequest submit(
      String bizType, String bizKey, String title, String payloadJson, Integer requiredApprovals, String remark) {
    if (!StringUtils.hasText(bizType) || !StringUtils.hasText(bizKey)
        || !StringUtils.hasText(title) || !StringUtils.hasText(payloadJson)) {
      throw new IllegalArgumentException("approval bizType, bizKey, title and payloadJson are required");
    }
    ApprovalFlow flow = resolveFlow(bizType);
    LocalDateTime now = LocalDateTime.now();
    ManagerApprovalRequest request = new ManagerApprovalRequest();
    request.setApprovalNo(newApprovalNo());
    request.setBizType(bizType.trim().toUpperCase());
    request.setBizKey(bizKey.trim());
    request.setDefinitionCode(flow.definitionCode());
    request.setTitle(title.trim());
    request.setStatus("PENDING");
    request.setCurrentStepNo(flow.firstStep().getStepNo());
    request.setTotalSteps(flow.steps().size());
    request.setRequiredApprovals(flow.firstStep().getRequiredApprovals());
    request.setApprovedCount(0);
    request.setApplicant(currentUsername());
    request.setPayloadJson(payloadJson);
    request.setRemark(trimToNull(remark));
    request.setCreatedAt(now);
    request.setUpdatedAt(now);
    requestMapper.insert(request);
    auditService.record("APPROVAL_SUBMITTED", "MANAGER_APPROVAL", request.getApprovalNo(), request.getStatus(), request);
    notifyCurrentStepApproversIfEnabled(request);
    return request;
  }

  /** 判断指定业务类型是否配置了启用审批流；没有配置时业务可直接放行。 */
  public boolean hasEnabledFlow(String bizType) {
    return definitionMapper.selectCount(Wrappers.<ManagerApprovalDefinition>lambdaQuery()
        .eq(ManagerApprovalDefinition::getBizType, normalize(bizType))
        .eq(ManagerApprovalDefinition::getEnabled, true)) > 0;
  }

  /** 返回审批配置页面的业务类型、审批人和审批角色候选项。 */
  public ApprovalOptionsResponse options() {
    List<ApprovalOption> bizTypes = List.of(
        new ApprovalOption("商户余额调账", "MERCHANT_BALANCE_ADJUST"));
    List<ApprovalOption> users = adminUserMapper.selectList(Wrappers.<ManagerAdminUser>lambdaQuery()
            .eq(ManagerAdminUser::getEnabled, Boolean.TRUE)
            .orderByAsc(ManagerAdminUser::getUsername))
        .stream()
        .map(user -> new ApprovalOption(user.getUsername(), user.getUsername()))
        .toList();
    List<ApprovalOption> roles = roleMapper.selectList(Wrappers.<ManagerRole>lambdaQuery()
            .eq(ManagerRole::getEnabled, Boolean.TRUE)
            .orderByAsc(ManagerRole::getRoleCode))
        .stream()
        .map(role -> new ApprovalOption(role.getRoleName() + " / " + role.getRoleCode(), role.getRoleCode()))
        .toList();
    return new ApprovalOptionsResponse(bizTypes, users, roles);
  }

  /** 分页查询审批申请。 */
  public ManagerPageResponse<ApprovalRequestView> pageApprovals(
      long page, long size, String bizType, String status) {
    LambdaQueryWrapper<ManagerApprovalRequest> query = Wrappers.<ManagerApprovalRequest>lambdaQuery()
        .eq(StringUtils.hasText(bizType), ManagerApprovalRequest::getBizType, normalize(bizType))
        .eq(StringUtils.hasText(status), ManagerApprovalRequest::getStatus, normalize(status))
        .orderByDesc(ManagerApprovalRequest::getCreatedAt)
        .orderByDesc(ManagerApprovalRequest::getId);
    Page<ManagerApprovalRequest> result =
        requestMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(ApprovalRequestView::from).toList());
  }

  /** 查询审批详情和已操作人。 */
  public ApprovalDetailResponse detail(String approvalNo) {
    ManagerApprovalRequest request = require(findRequest(approvalNo), "approval request not found");
    List<ApprovalActionView> actions = actionMapper.selectList(Wrappers.<ManagerApprovalAction>lambdaQuery()
            .eq(ManagerApprovalAction::getApprovalNo, request.getApprovalNo())
            .orderByAsc(ManagerApprovalAction::getCreatedAt))
        .stream()
        .map(ApprovalActionView::from)
        .toList();
    return new ApprovalDetailResponse(ApprovalRequestView.from(request), actions);
  }

  /** 分页查询审批流定义。 */
  public ManagerPageResponse<ApprovalDefinitionView> pageDefinitions(long page, long size, String bizType) {
    Page<ManagerApprovalDefinition> result = definitionMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)),
        Wrappers.<ManagerApprovalDefinition>lambdaQuery()
            .eq(StringUtils.hasText(bizType), ManagerApprovalDefinition::getBizType, normalize(bizType))
            .orderByAsc(ManagerApprovalDefinition::getBizType)
            .orderByAsc(ManagerApprovalDefinition::getDefinitionCode));
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(ApprovalDefinitionView::from).toList());
  }

  /** 保存审批流定义。 */
  @Transactional(rollbackFor = Exception.class)
  public ApprovalDefinitionView saveDefinition(ApprovalDefinitionSaveRequest request) {
    if (request == null || !StringUtils.hasText(request.definitionCode()) || !StringUtils.hasText(request.bizType())) {
      throw new IllegalArgumentException("definitionCode and bizType are required");
    }
    ManagerApprovalDefinition existing = request.id() == null
        ? definitionMapper.selectOne(Wrappers.<ManagerApprovalDefinition>lambdaQuery()
            .eq(ManagerApprovalDefinition::getDefinitionCode, normalize(request.definitionCode()))
            .last("limit 1"))
        : definitionMapper.selectById(request.id());
    ManagerApprovalDefinition definition = existing == null ? new ManagerApprovalDefinition() : existing;
    LocalDateTime now = LocalDateTime.now();
    definition.setDefinitionCode(normalize(request.definitionCode()));
    definition.setBizType(normalize(request.bizType()));
    definition.setDefinitionName(StringUtils.hasText(request.definitionName()) ? request.definitionName().trim() : definition.getDefinitionCode());
    definition.setNotifyNextApprover(Boolean.TRUE.equals(request.notifyNextApprover()));
    definition.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    definition.setUpdatedAt(now);
    if (definition.getId() == null) {
      definition.setCreatedAt(now);
      definitionMapper.insert(definition);
    } else {
      definitionMapper.updateById(definition);
    }
    if (request.steps() != null && !request.steps().isEmpty()) {
      stepMapper.delete(Wrappers.<ManagerApprovalStepDefinition>lambdaQuery()
          .eq(ManagerApprovalStepDefinition::getDefinitionCode, definition.getDefinitionCode()));
      for (ApprovalStepSaveRequest step : request.steps()) {
        saveStep(new ApprovalStepSaveRequest(
            null,
            definition.getDefinitionCode(),
            step.stepNo(),
            step.stepName(),
            step.requiredApprovals(),
            step.approverUsers(),
            step.approverRoles(),
            step.enabled()));
      }
    }
    return ApprovalDefinitionView.from(definition);
  }

  /** 查询某个审批流下的步骤配置。 */
  public List<ApprovalStepView> steps(String definitionCode) {
    return stepDefinitions(definitionCode).stream().map(ApprovalStepView::from).toList();
  }

  /** 保存审批步骤。 */
  @Transactional(rollbackFor = Exception.class)
  public ApprovalStepView saveStep(ApprovalStepSaveRequest request) {
    if (request == null || !StringUtils.hasText(request.definitionCode()) || request.stepNo() == null
        || request.stepNo() < 1) {
      throw new IllegalArgumentException("definitionCode and positive stepNo are required");
    }
    String definitionCode = normalize(request.definitionCode());
    require(definitionMapper.selectOne(Wrappers.<ManagerApprovalDefinition>lambdaQuery()
        .eq(ManagerApprovalDefinition::getDefinitionCode, definitionCode)
        .last("limit 1")), "approval definition not found");
    ManagerApprovalStepDefinition existing = request.id() == null
        ? stepMapper.selectOne(Wrappers.<ManagerApprovalStepDefinition>lambdaQuery()
            .eq(ManagerApprovalStepDefinition::getDefinitionCode, definitionCode)
            .eq(ManagerApprovalStepDefinition::getStepNo, request.stepNo())
            .last("limit 1"))
        : stepMapper.selectById(request.id());
    ManagerApprovalStepDefinition step = existing == null ? new ManagerApprovalStepDefinition() : existing;
    LocalDateTime now = LocalDateTime.now();
    step.setDefinitionCode(definitionCode);
    step.setStepNo(request.stepNo());
    step.setStepName(StringUtils.hasText(request.stepName()) ? request.stepName().trim() : "Step " + request.stepNo());
    step.setRequiredApprovals(Math.max(request.requiredApprovals() == null ? MIN_REQUIRED_APPROVALS : request.requiredApprovals(), 1));
    step.setApproverUsers(trimToNull(request.approverUsers()));
    step.setApproverRoles(trimToNull(request.approverRoles()));
    step.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    step.setUpdatedAt(now);
    if (step.getId() == null) {
      step.setCreatedAt(now);
      stepMapper.insert(step);
    } else {
      stepMapper.updateById(step);
    }
    return ApprovalStepView.from(step);
  }

  /** 审批通过；达到配置人数后执行真实业务动作。 */
  @Transactional(rollbackFor = Exception.class)
  public ApprovalRequestView approve(String approvalNo, ApprovalActionRequest request) {
    ManagerApprovalRequest approval = requirePending(approvalNo);
    String approver = currentUsername();
    if (approver.equalsIgnoreCase(approval.getApplicant())) {
      throw new IllegalArgumentException("applicant cannot approve own request");
    }
    ManagerApprovalStepDefinition step = currentStep(approval);
    requireApproverAllowed(step, ManagerSecurityContext.requireCurrent());
    insertAction(approval.getApprovalNo(), approval.getCurrentStepNo(), approver, "APPROVE", request == null ? null : request.comment());
    int approvedCount = countApproved(approval.getApprovalNo(), approval.getCurrentStepNo());
    approval.setApprovedCount(approvedCount);
    approval.setUpdatedAt(LocalDateTime.now());
    if (approvedCount >= approval.getRequiredApprovals()) {
      ManagerApprovalStepDefinition nextStep = nextStep(approval);
      if (nextStep == null) {
        approval.setStatus("APPROVED");
        approval.setCompletedAt(LocalDateTime.now());
        handler(approval.getBizType()).executeApproved(approval);
      } else {
        approval.setCurrentStepNo(nextStep.getStepNo());
        approval.setRequiredApprovals(nextStep.getRequiredApprovals());
        approval.setApprovedCount(0);
        notifyCurrentStepApproversIfEnabled(approval);
      }
    }
    requestMapper.updateById(approval);
    auditService.record("APPROVAL_APPROVED", "MANAGER_APPROVAL", approval.getApprovalNo(), approval.getStatus(), approval);
    return ApprovalRequestView.from(approval);
  }

  /** 任意一个有权限的审批人拒绝后，审批单结束。 */
  @Transactional(rollbackFor = Exception.class)
  public ApprovalRequestView reject(String approvalNo, ApprovalActionRequest request) {
    ManagerApprovalRequest approval = requirePending(approvalNo);
    String approver = currentUsername();
    if (approver.equalsIgnoreCase(approval.getApplicant())) {
      throw new IllegalArgumentException("applicant cannot reject own request");
    }
    String reason = request == null ? null : request.rejectReason();
    requireApproverAllowed(currentStep(approval), ManagerSecurityContext.requireCurrent());
    insertAction(approval.getApprovalNo(), approval.getCurrentStepNo(), approver, "REJECT", StringUtils.hasText(reason) ? reason : null);
    approval.setStatus("REJECTED");
    approval.setRejectReason(StringUtils.hasText(reason) ? reason.trim() : null);
    approval.setCompletedAt(LocalDateTime.now());
    approval.setUpdatedAt(LocalDateTime.now());
    requestMapper.updateById(approval);
    auditService.record("APPROVAL_REJECTED", "MANAGER_APPROVAL", approval.getApprovalNo(), approval.getStatus(), approval);
    return ApprovalRequestView.from(approval);
  }

  private ManagerApprovalRequest requirePending(String approvalNo) {
    ManagerApprovalRequest approval = require(findRequest(approvalNo), "approval request not found");
    if (!"PENDING".equals(approval.getStatus())) {
      throw new IllegalArgumentException("approval request is not pending");
    }
    return approval;
  }

  private ManagerApprovalRequest findRequest(String approvalNo) {
    if (!StringUtils.hasText(approvalNo)) {
      return null;
    }
    return requestMapper.selectOne(Wrappers.<ManagerApprovalRequest>lambdaQuery()
        .eq(ManagerApprovalRequest::getApprovalNo, approvalNo.trim())
        .last("limit 1"));
  }

  private void insertAction(String approvalNo, Integer stepNo, String approver, String action, String comment) {
    Long existingActions = actionMapper.selectCount(Wrappers.<ManagerApprovalAction>lambdaQuery()
        .eq(ManagerApprovalAction::getApprovalNo, approvalNo)
        .eq(ManagerApprovalAction::getApprover, approver));
    if (existingActions != null && existingActions > 0) {
      throw new IllegalArgumentException("current manager has already reviewed this request");
    }
    ManagerApprovalAction record = new ManagerApprovalAction();
    record.setApprovalNo(approvalNo);
    record.setStepNo(stepNo);
    record.setApprover(approver);
    record.setAction(action);
    record.setComment(StringUtils.hasText(comment) ? comment.trim() : null);
    record.setCreatedAt(LocalDateTime.now());
    try {
      actionMapper.insert(record);
    } catch (DuplicateKeyException e) {
      throw new IllegalArgumentException("current manager has already reviewed this request");
    }
  }

  private int countApproved(String approvalNo, Integer stepNo) {
    return Math.toIntExact(actionMapper.selectCount(Wrappers.<ManagerApprovalAction>lambdaQuery()
        .eq(ManagerApprovalAction::getApprovalNo, approvalNo)
        .eq(ManagerApprovalAction::getStepNo, stepNo)
        .eq(ManagerApprovalAction::getAction, "APPROVE")));
  }

  private ApprovalFlow resolveFlow(String bizType) {
    ManagerApprovalDefinition definition = definitionMapper.selectOne(Wrappers.<ManagerApprovalDefinition>lambdaQuery()
        .eq(ManagerApprovalDefinition::getBizType, normalize(bizType))
        .eq(ManagerApprovalDefinition::getEnabled, true)
        .orderByDesc(ManagerApprovalDefinition::getUpdatedAt)
        .last("limit 1"));
    if (definition == null) {
      throw new IllegalArgumentException("approval definition is not enabled: " + normalize(bizType));
    }
    List<ManagerApprovalStepDefinition> steps = stepDefinitions(definition.getDefinitionCode());
    if (steps.isEmpty()) {
      throw new IllegalArgumentException("approval definition has no enabled step: " + definition.getDefinitionCode());
    }
    return new ApprovalFlow(definition.getDefinitionCode(), steps);
  }

  private List<ManagerApprovalStepDefinition> stepDefinitions(String definitionCode) {
    return stepMapper.selectList(Wrappers.<ManagerApprovalStepDefinition>lambdaQuery()
        .eq(ManagerApprovalStepDefinition::getDefinitionCode, normalize(definitionCode))
        .eq(ManagerApprovalStepDefinition::getEnabled, true)
        .orderByAsc(ManagerApprovalStepDefinition::getStepNo));
  }

  private ManagerApprovalStepDefinition currentStep(ManagerApprovalRequest approval) {
    return stepDefinitions(approval.getDefinitionCode()).stream()
        .filter(step -> step.getStepNo().equals(approval.getCurrentStepNo()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("current approval step not found"));
  }

  private ManagerApprovalStepDefinition nextStep(ManagerApprovalRequest approval) {
    return stepDefinitions(approval.getDefinitionCode()).stream()
        .filter(step -> step.getStepNo() > approval.getCurrentStepNo())
        .findFirst()
        .orElse(null);
  }

  private void requireApproverAllowed(ManagerApprovalStepDefinition step, ManagerPrincipal principal) {
    if (principal.admin()) {
      return;
    }
    boolean userConfigured = StringUtils.hasText(step.getApproverUsers());
    boolean roleConfigured = StringUtils.hasText(step.getApproverRoles());
    if (!userConfigured && !roleConfigured) {
      return;
    }
    if (userConfigured && containsCsv(step.getApproverUsers(), principal.getUsername())) {
      return;
    }
    if (roleConfigured && principal.roleCodes().stream().anyMatch(role -> containsCsv(step.getApproverRoles(), role))) {
      return;
    }
    throw new IllegalArgumentException("current manager is not allowed to review this approval step");
  }

  private boolean containsCsv(String csv, String value) {
    if (!StringUtils.hasText(csv) || !StringUtils.hasText(value)) {
      return false;
    }
    String normalizedValue = value.trim();
    for (String item : csv.split(",")) {
      if (normalizedValue.equalsIgnoreCase(item.trim())) {
        return true;
      }
    }
    return false;
  }

  private void notifyCurrentStepApproversIfEnabled(ManagerApprovalRequest approval) {
    if (!notifyNextApproverEnabled(approval.getDefinitionCode())) {
      log.debug("Approval next approver notification skipped by definition switch. approvalNo={}, definitionCode={}",
          approval.getApprovalNo(), approval.getDefinitionCode());
      return;
    }
    ManagerApprovalStepDefinition step = currentStep(approval);
    for (ManagerAdminUser user : resolveApproverUsers(step)) {
      if (!StringUtils.hasText(user.getContactType()) || !StringUtils.hasText(user.getContactValue())) {
        continue;
      }
      try {
        messageService.sendByTemplate(
            "APPROVAL_PENDING",
            user.getContactType(),
            user.getContactValue(),
            Map.of(
                "approvalNo", approval.getApprovalNo(),
                "bizType", approval.getBizType(),
                "title", approval.getTitle(),
                "stepName", step.getStepName()));
      } catch (RuntimeException e) {
        log.warn("Approval next approver notification failed. approvalNo={}, approver={}, channel={}",
            approval.getApprovalNo(), user.getUsername(), user.getContactType(), e);
      }
    }
  }

  private boolean notifyNextApproverEnabled(String definitionCode) {
    if (!StringUtils.hasText(definitionCode)) {
      return false;
    }
    ManagerApprovalDefinition definition = definitionMapper.selectOne(Wrappers.<ManagerApprovalDefinition>lambdaQuery()
        .eq(ManagerApprovalDefinition::getDefinitionCode, normalize(definitionCode))
        .last("limit 1"));
    return definition != null && Boolean.TRUE.equals(definition.getNotifyNextApprover());
  }

  private List<ManagerAdminUser> resolveApproverUsers(ManagerApprovalStepDefinition step) {
    Set<Long> userIds = new LinkedHashSet<>();
    for (String username : csvItems(step.getApproverUsers(), false)) {
      ManagerAdminUser user = adminUserMapper.selectOne(Wrappers.<ManagerAdminUser>lambdaQuery()
          .eq(ManagerAdminUser::getUsername, username)
          .eq(ManagerAdminUser::getEnabled, Boolean.TRUE)
          .last("limit 1"));
      if (user != null) {
        userIds.add(user.getId());
      }
    }
    for (String roleCode : csvItems(step.getApproverRoles(), true)) {
      userRoleMapper.selectList(Wrappers.<ManagerUserRole>lambdaQuery()
              .eq(ManagerUserRole::getRoleCode, roleCode))
          .forEach(binding -> userIds.add(binding.getUserId()));
    }
    if (userIds.isEmpty() && !StringUtils.hasText(step.getApproverUsers()) && !StringUtils.hasText(step.getApproverRoles())) {
      return adminUserMapper.selectList(Wrappers.<ManagerAdminUser>lambdaQuery()
          .eq(ManagerAdminUser::getEnabled, Boolean.TRUE)
          .last("limit 50"));
    }
    if (userIds.isEmpty()) {
      return List.of();
    }
    return adminUserMapper.selectList(Wrappers.<ManagerAdminUser>lambdaQuery()
        .in(ManagerAdminUser::getId, userIds)
        .eq(ManagerAdminUser::getEnabled, Boolean.TRUE));
  }

  private List<String> csvItems(String csv, boolean upperCase) {
    if (!StringUtils.hasText(csv)) {
      return List.of();
    }
    return java.util.Arrays.stream(csv.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(value -> upperCase ? value.toUpperCase() : value)
        .distinct()
        .toList();
  }

  private ApprovalBusinessHandler handler(String bizType) {
    ApprovalBusinessHandler handler = handlers.get(normalize(bizType));
    if (handler == null) {
      throw new IllegalArgumentException("approval business handler not found: " + bizType);
    }
    return handler;
  }

  private String newApprovalNo() {
    return "AP" + LocalDateTime.now().format(NO_TIME)
        + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private String currentUsername() {
    return ManagerSecurityContext.requireCurrent().getUsername();
  }

  private record ApprovalFlow(String definitionCode, List<ManagerApprovalStepDefinition> steps) {
    private ManagerApprovalStepDefinition firstStep() {
      return steps.get(0);
    }
  }
}
