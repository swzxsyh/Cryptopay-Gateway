package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.DataPermissionSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.FunctionPermissionOption;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.FunctionPermissionTreeNode;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.FunctionPermissionSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.RoleDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.RoleSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.RoleWithFunctionPermissionsSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerRoleDtos.UserRoleAssignRequest;
import io.swzxsyh.manager.security.ManagerPermission;
import io.swzxsyh.manager.security.entity.ManagerRole;
import io.swzxsyh.manager.security.entity.ManagerRoleDataPermission;
import io.swzxsyh.manager.security.entity.ManagerRoleFunctionPermission;
import io.swzxsyh.manager.security.entity.ManagerUserRole;
import io.swzxsyh.manager.security.mapper.ManagerRoleDataPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleFunctionPermissionMapper;
import io.swzxsyh.manager.security.mapper.ManagerRoleMapper;
import io.swzxsyh.manager.security.mapper.ManagerUserRoleMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 管理端角色、功能权限、数据权限和用户角色绑定维护服务。 */
@Service
public class ManagerRoleApplicationService extends ManagerApplicationSupport {

  private final ManagerRoleMapper roleMapper;
  private final ManagerUserRoleMapper userRoleMapper;
  private final ManagerRoleFunctionPermissionMapper functionPermissionMapper;
  private final ManagerRoleDataPermissionMapper dataPermissionMapper;

  public ManagerRoleApplicationService(
      ManagerRoleMapper roleMapper,
      ManagerUserRoleMapper userRoleMapper,
      ManagerRoleFunctionPermissionMapper functionPermissionMapper,
      ManagerRoleDataPermissionMapper dataPermissionMapper) {
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.functionPermissionMapper = functionPermissionMapper;
    this.dataPermissionMapper = dataPermissionMapper;
  }

  /** 分页查询角色。 */
  public ManagerPageResponse<ManagerRole> pageRoles(long page, long size, Boolean enabled) {
    LambdaQueryWrapper<ManagerRole> query = Wrappers.<ManagerRole>lambdaQuery()
        .eq(enabled != null, ManagerRole::getEnabled, enabled)
        .orderByAsc(ManagerRole::getRoleCode);
    return page(roleMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query));
  }

  /** 查询角色详情。 */
  public RoleDetailResponse roleDetail(String roleCode) {
    String normalizedRoleCode = normalizeRoleCode(roleCode);
    ManagerRole role = require(findRole(normalizedRoleCode), "manager role not found");
    return new RoleDetailResponse(
        role,
        functionPermissionMapper.selectList(Wrappers.<ManagerRoleFunctionPermission>lambdaQuery()
            .eq(ManagerRoleFunctionPermission::getRoleCode, normalizedRoleCode)
            .orderByAsc(ManagerRoleFunctionPermission::getPermissionCode)),
        dataPermissionMapper.selectList(Wrappers.<ManagerRoleDataPermission>lambdaQuery()
            .eq(ManagerRoleDataPermission::getRoleCode, normalizedRoleCode)
            .orderByAsc(ManagerRoleDataPermission::getScopeType)
            .orderByAsc(ManagerRoleDataPermission::getScopeValue)));
  }

  /** 新增或更新角色。 */
  public ManagerRole saveRole(RoleSaveRequest request) {
    if (request == null || !hasText(request.roleCode()) || !hasText(request.roleName())) {
      throw new IllegalArgumentException("roleCode and roleName are required");
    }
    String roleCode = normalizeRoleCode(request.roleCode());
    LocalDateTime now = LocalDateTime.now();
    ManagerRole existing = findRole(roleCode);
    ManagerRole role = existing == null ? new ManagerRole() : existing;
    role.setRoleCode(roleCode);
    role.setRoleName(request.roleName().trim());
    role.setSuperAdmin(Boolean.TRUE.equals(request.superAdmin()));
    role.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    role.setUpdatedAt(now);
    if (existing == null) {
      role.setCreatedAt(now);
      roleMapper.insert(role);
    } else {
      roleMapper.updateById(role);
    }
    return role;
  }

  /** 返回系统内置功能权限清单，供角色新增/编辑弹窗以勾选菜单展示。 */
  public List<FunctionPermissionOption> functionPermissionOptions() {
    return permissionCatalog().values().stream().toList();
  }

  /** 返回系统内置功能权限菜单树，父级用于分组，前端只保存叶子权限编码。 */
  public List<FunctionPermissionTreeNode> functionPermissionTree() {
    return List.of(
        group("console", "控制台",
            page(ManagerPermission.DASHBOARD_VIEW, "仪表盘查看")),
        group("paymentOps", "支付运营",
            page(ManagerPermission.ORDER_VIEW, "订单查看"),
            page(ManagerPermission.CALLBACK_MANAGE, "回调管理",
                leaf(ManagerPermission.CALLBACK_REPLAY, "重投回调")),
            page(ManagerPermission.PAYMENT_EXCEPTION_MANAGE, "异常单管理",
                leaf(ManagerPermission.PAYMENT_EXCEPTION_HANDLE, "处理异常单")),
            page(ManagerPermission.APPROVAL_MANAGE, "多人审批",
                leaf(ManagerPermission.APPROVAL_REVIEW, "审批操作"),
                leaf(ManagerPermission.APPROVAL_CONFIG_SAVE, "保存审批配置"))),
        group("ledger", "账务与流水",
            page(ManagerPermission.RAW_CHAIN_LOG_MANAGE, "链上流水管理",
                leaf(ManagerPermission.RAW_CHAIN_LOG_MANUAL_PROCESS, "人工处理流水")),
            page(ManagerPermission.RECONCILIATION_MANAGE, "对账管理",
                leaf(ManagerPermission.RECONCILIATION_RUN, "运行对账"),
                leaf(ManagerPermission.RECONCILIATION_MANUAL_CONFIRM, "人工确认对账")),
            page(ManagerPermission.SETTLEMENT_MANAGE, "清分结算管理",
                leaf(ManagerPermission.SETTLEMENT_MARK, "标记结算状态")),
            page(ManagerPermission.MERCHANT_BALANCE_MANAGE, "商户资金余额",
                leaf(ManagerPermission.MERCHANT_BALANCE_ADJUST, "商户余额调账"),
                leaf(ManagerPermission.MERCHANT_WITHDRAW_OPERATE, "审核/处理提现"))),
        group("merchant", "商户管理",
            page(ManagerPermission.MERCHANT_MANAGE, "商户主数据",
                leaf(ManagerPermission.MERCHANT_SAVE, "保存商户主数据"),
                leaf(ManagerPermission.MERCHANT_CHANNEL_SAVE, "保存商户通道费率")),
            page(ManagerPermission.MERCHANT_PRODUCT_MANAGE, "商户产品库",
                leaf(ManagerPermission.MERCHANT_PRODUCT_SAVE, "保存商户产品"))),
        group("chainOps", "链路与地址",
            page(ManagerPermission.ADDRESS_POOL_MANAGE, "地址池管理",
                leaf(ManagerPermission.ADDRESS_POOL_RELEASE, "释放地址租约"),
                leaf(ManagerPermission.ADDRESS_POOL_BLOCK, "封禁风险地址"),
                leaf(ManagerPermission.ADDRESS_POOL_RETIRE, "退役地址"),
                leaf(ManagerPermission.ADDRESS_POOL_POLICY_SAVE, "保存地址池策略")),
            page(ManagerPermission.SCANNER_MANAGE, "扫描器管理",
                leaf(ManagerPermission.SCANNER_CHECKPOINT_SAVE, "保存扫描检查点"))),
        group("subscription", "订阅支付",
            page(ManagerPermission.SUBSCRIPTION_MANAGE, "订阅管理",
                leaf(ManagerPermission.SUBSCRIPTION_ORDER_OPERATE, "暂停/恢复/取消订阅"),
                leaf(ManagerPermission.SUBSCRIPTION_BILLING_OPERATE, "处理订阅账单"))),
        group("riskAudit", "风控审计",
            page(ManagerPermission.RISK_MANAGE, "风控管理",
                leaf(ManagerPermission.RISK_RULE_SAVE, "保存风控规则"),
                leaf(ManagerPermission.RISK_RULE_DELETE, "删除风控规则")),
            page(ManagerPermission.AUDIT_VIEW, "审计查看")),
        group("system", "系统设置",
            page(ManagerPermission.CONFIG_MANAGE, "系统配置管理",
                leaf(ManagerPermission.CONFIG_SAVE, "保存运行配置"),
                leaf(ManagerPermission.TOKEN_ONBOARD, "新增代币接入")),
            page("security:manage", ManagerPermission.CONFIG_MANAGE, "角色与用户管理",
                leaf(ManagerPermission.ROLE_SAVE, "保存角色权限"),
                leaf(ManagerPermission.USER_SAVE, "保存用户权限"))));
  }

  /** 新增或更新角色，并在同一事务内覆盖角色功能权限。 */
  @Transactional(rollbackFor = Exception.class)
  public RoleDetailResponse saveRoleWithFunctionPermissions(RoleWithFunctionPermissionsSaveRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("role request is required");
    }
    ManagerRole role = saveRole(new RoleSaveRequest(
        request.roleCode(),
        request.roleName(),
        request.superAdmin(),
        request.enabled()));
    replaceFunctionPermissions(role.getRoleCode(), request.functionPermissionCodes(), LocalDateTime.now());
    return roleDetail(role.getRoleCode());
  }

  /** 新增或更新角色功能权限。 */
  public ManagerRoleFunctionPermission saveFunctionPermission(
      String roleCode, FunctionPermissionSaveRequest request) {
    String normalizedRoleCode = normalizeRoleCode(roleCode);
    require(findRole(normalizedRoleCode), "manager role not found");
    if (request == null || !hasText(request.permissionCode())) {
      throw new IllegalArgumentException("permissionCode is required");
    }
    String permissionCode = request.permissionCode().trim();
    LocalDateTime now = LocalDateTime.now();
    ManagerRoleFunctionPermission existing = functionPermissionMapper.selectOne(
        Wrappers.<ManagerRoleFunctionPermission>lambdaQuery()
            .eq(ManagerRoleFunctionPermission::getRoleCode, normalizedRoleCode)
            .eq(ManagerRoleFunctionPermission::getPermissionCode, permissionCode)
            .last("limit 1"));
    ManagerRoleFunctionPermission permission =
        existing == null ? new ManagerRoleFunctionPermission() : existing;
    permission.setRoleCode(normalizedRoleCode);
    permission.setPermissionCode(permissionCode);
    permission.setPermissionName(request.permissionName());
    permission.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    permission.setUpdatedAt(now);
    if (existing == null) {
      permission.setCreatedAt(now);
      functionPermissionMapper.insert(permission);
    } else {
      functionPermissionMapper.updateById(permission);
    }
    return permission;
  }

  /** 新增或更新角色数据权限。 */
  public ManagerRoleDataPermission saveDataPermission(
      String roleCode, DataPermissionSaveRequest request) {
    String normalizedRoleCode = normalizeRoleCode(roleCode);
    require(findRole(normalizedRoleCode), "manager role not found");
    if (request == null || !hasText(request.scopeType()) || !hasText(request.scopeValue())) {
      throw new IllegalArgumentException("scopeType and scopeValue are required");
    }
    String scopeType = request.scopeType().trim().toUpperCase();
    String scopeValue = "ALL".equals(scopeType) ? "*" : request.scopeValue().trim();
    LocalDateTime now = LocalDateTime.now();
    cleanConflictingDataScopes(normalizedRoleCode, scopeType);
    ManagerRoleDataPermission existing = dataPermissionMapper.selectOne(
        Wrappers.<ManagerRoleDataPermission>lambdaQuery()
            .eq(ManagerRoleDataPermission::getRoleCode, normalizedRoleCode)
            .eq(ManagerRoleDataPermission::getScopeType, scopeType)
            .eq(ManagerRoleDataPermission::getScopeValue, scopeValue)
            .last("limit 1"));
    ManagerRoleDataPermission permission = existing == null ? new ManagerRoleDataPermission() : existing;
    permission.setRoleCode(normalizedRoleCode);
    permission.setScopeType(scopeType);
    permission.setScopeValue(scopeValue);
    permission.setEnabled(request.enabled() == null || Boolean.TRUE.equals(request.enabled()));
    permission.setUpdatedAt(now);
    if (existing == null) {
      permission.setCreatedAt(now);
      dataPermissionMapper.insert(permission);
    } else {
      dataPermissionMapper.updateById(permission);
    }
    return permission;
  }

  /**
   * 数据权限采用互斥语义：
   * 保存 ALL/* 时清理同角色下所有指定范围；保存指定商户/链时清理同角色下 ALL/*。
   */
  private void cleanConflictingDataScopes(String roleCode, String scopeType) {
    if ("ALL".equals(scopeType)) {
      dataPermissionMapper.delete(Wrappers.<ManagerRoleDataPermission>lambdaQuery()
          .eq(ManagerRoleDataPermission::getRoleCode, roleCode)
          .ne(ManagerRoleDataPermission::getScopeType, "ALL"));
      return;
    }
    dataPermissionMapper.delete(Wrappers.<ManagerRoleDataPermission>lambdaQuery()
        .eq(ManagerRoleDataPermission::getRoleCode, roleCode)
        .eq(ManagerRoleDataPermission::getScopeType, "ALL"));
  }

  /** 给管理员账号重新分配角色。 */
  @Transactional
  public void assignUserRoles(Long userId, UserRoleAssignRequest request) {
    if (userId == null || request == null || request.roleCodes() == null || request.roleCodes().isEmpty()) {
      throw new IllegalArgumentException("userId and roleCodes are required");
    }
    userRoleMapper.delete(Wrappers.<ManagerUserRole>lambdaQuery().eq(ManagerUserRole::getUserId, userId));
    LocalDateTime now = LocalDateTime.now();
    for (String roleCode : request.roleCodes()) {
      String normalizedRoleCode = normalizeRoleCode(roleCode);
      require(findRole(normalizedRoleCode), "manager role not found: " + normalizedRoleCode);
      ManagerUserRole binding = new ManagerUserRole();
      binding.setUserId(userId);
      binding.setRoleCode(normalizedRoleCode);
      binding.setCreatedAt(now);
      userRoleMapper.insert(binding);
    }
  }

  private ManagerRole findRole(String roleCode) {
    return roleMapper.selectOne(Wrappers.<ManagerRole>lambdaQuery()
        .eq(ManagerRole::getRoleCode, roleCode)
        .last("limit 1"));
  }

  private void replaceFunctionPermissions(String roleCode, List<String> permissionCodes, LocalDateTime now) {
    functionPermissionMapper.delete(Wrappers.<ManagerRoleFunctionPermission>lambdaQuery()
        .eq(ManagerRoleFunctionPermission::getRoleCode, roleCode));
    Map<String, FunctionPermissionOption> catalog = permissionCatalog();
    for (String permissionCode : normalizePermissionCodes(permissionCodes)) {
      FunctionPermissionOption option = catalog.get(permissionCode);
      if (option == null) {
        throw new IllegalArgumentException("unknown function permission: " + permissionCode);
      }
      ManagerRoleFunctionPermission permission = new ManagerRoleFunctionPermission();
      permission.setRoleCode(roleCode);
      permission.setPermissionCode(option.permissionCode());
      permission.setPermissionName(option.permissionName());
      permission.setEnabled(Boolean.TRUE);
      permission.setCreatedAt(now);
      permission.setUpdatedAt(now);
      functionPermissionMapper.insert(permission);
    }
  }

  private List<String> normalizePermissionCodes(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream()
        .filter(this::hasText)
        .map(value -> value.trim().toLowerCase())
        .distinct()
        .toList();
  }

  private Map<String, FunctionPermissionOption> permissionCatalog() {
    Map<String, FunctionPermissionOption> options = new LinkedHashMap<>();
    addPermission(options, ManagerPermission.DASHBOARD_VIEW, "仪表盘查看");
    addPermission(options, ManagerPermission.ORDER_VIEW, "订单查看");
    addPermission(options, ManagerPermission.CALLBACK_MANAGE, "回调管理");
    addPermission(options, ManagerPermission.CALLBACK_REPLAY, "重投回调");
    addPermission(options, ManagerPermission.APPROVAL_MANAGE, "多人审批");
    addPermission(options, ManagerPermission.APPROVAL_REVIEW, "审批操作");
    addPermission(options, ManagerPermission.APPROVAL_CONFIG_SAVE, "保存审批配置");
    addPermission(options, ManagerPermission.AUDIT_VIEW, "审计查看");
    addPermission(options, ManagerPermission.PAYMENT_EXCEPTION_MANAGE, "异常单管理");
    addPermission(options, ManagerPermission.PAYMENT_EXCEPTION_HANDLE, "处理异常单");
    addPermission(options, ManagerPermission.RAW_CHAIN_LOG_MANAGE, "链上流水管理");
    addPermission(options, ManagerPermission.RAW_CHAIN_LOG_MANUAL_PROCESS, "人工处理流水");
    addPermission(options, ManagerPermission.RECONCILIATION_MANAGE, "对账管理");
    addPermission(options, ManagerPermission.RECONCILIATION_RUN, "运行对账");
    addPermission(options, ManagerPermission.RECONCILIATION_MANUAL_CONFIRM, "人工确认对账");
    addPermission(options, ManagerPermission.MERCHANT_MANAGE, "商户主数据管理");
    addPermission(options, ManagerPermission.MERCHANT_SAVE, "保存商户主数据");
    addPermission(options, ManagerPermission.MERCHANT_BALANCE_MANAGE, "商户资金余额管理");
    addPermission(options, ManagerPermission.MERCHANT_BALANCE_ADJUST, "商户余额调账");
    addPermission(options, ManagerPermission.CONFIG_MANAGE, "系统配置管理");
    addPermission(options, ManagerPermission.CONFIG_SAVE, "保存运行配置");
    addPermission(options, ManagerPermission.TOKEN_ONBOARD, "新增代币接入");
    addPermission(options, ManagerPermission.RISK_MANAGE, "风控管理");
    addPermission(options, ManagerPermission.RISK_RULE_SAVE, "保存风控规则");
    addPermission(options, ManagerPermission.RISK_RULE_DELETE, "删除风控规则");
    addPermission(options, ManagerPermission.SCANNER_MANAGE, "扫描器管理");
    addPermission(options, ManagerPermission.SCANNER_CHECKPOINT_SAVE, "保存扫描检查点");
    addPermission(options, ManagerPermission.SUBSCRIPTION_MANAGE, "订阅管理");
    addPermission(options, ManagerPermission.SUBSCRIPTION_ORDER_OPERATE, "订阅订单操作");
    addPermission(options, ManagerPermission.SUBSCRIPTION_BILLING_OPERATE, "订阅账单操作");
    addPermission(options, ManagerPermission.ADDRESS_POOL_MANAGE, "地址池管理");
    addPermission(options, ManagerPermission.ADDRESS_POOL_RELEASE, "释放地址租约");
    addPermission(options, ManagerPermission.ADDRESS_POOL_BLOCK, "封禁风险地址");
    addPermission(options, ManagerPermission.ADDRESS_POOL_RETIRE, "退役地址");
    addPermission(options, ManagerPermission.ADDRESS_POOL_POLICY_SAVE, "保存地址池策略");
    addPermission(options, ManagerPermission.SETTLEMENT_MANAGE, "清分结算管理");
    addPermission(options, ManagerPermission.SETTLEMENT_MARK, "标记结算状态");
    addPermission(options, ManagerPermission.ROLE_SAVE, "保存角色权限");
    addPermission(options, ManagerPermission.USER_SAVE, "保存用户权限");
    return options;
  }

  private void addPermission(
      Map<String, FunctionPermissionOption> options, String permissionCode, String permissionName) {
    options.put(permissionCode, new FunctionPermissionOption(permissionCode, permissionName));
  }

  private FunctionPermissionTreeNode group(
      String groupCode, String groupName, FunctionPermissionTreeNode... children) {
    return new FunctionPermissionTreeNode("group:" + groupCode, groupName, null, List.of(children));
  }

  private FunctionPermissionTreeNode leaf(String permissionCode, String permissionName) {
    return new FunctionPermissionTreeNode(permissionCode, permissionName, permissionCode, List.of());
  }

  private FunctionPermissionTreeNode page(
      String permissionCode, String permissionName, FunctionPermissionTreeNode... children) {
    return new FunctionPermissionTreeNode(permissionCode, permissionName, permissionCode, List.of(children));
  }

  private FunctionPermissionTreeNode page(
      String key, String permissionCode, String permissionName, FunctionPermissionTreeNode... children) {
    return new FunctionPermissionTreeNode(key, permissionName, permissionCode, List.of(children));
  }

  private String normalizeRoleCode(String roleCode) {
    if (!hasText(roleCode)) {
      throw new IllegalArgumentException("roleCode is required");
    }
    return roleCode.trim().toUpperCase();
  }
}
