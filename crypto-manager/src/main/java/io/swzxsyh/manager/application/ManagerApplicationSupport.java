package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.security.ManagerDataScope;
import io.swzxsyh.manager.security.ManagerPrincipal;
import io.swzxsyh.manager.security.ManagerSecurityContext;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.util.StringUtils;

/** 管理端应用服务公共辅助方法，统一分页、空值校验和关联记录查询。 */
abstract class ManagerApplicationSupport {

  /** 将 MyBatis Plus 分页结果转换为管理端统一分页响应。 */
  protected <T> ManagerPageResponse<T> page(Page<T> result) {
    return new ManagerPageResponse<>(
        result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
  }

  /** 规范化页码，避免非法页码传入数据库分页。 */
  protected long normalizePage(long page) {
    return Math.max(1, page);
  }

  /** 规范化每页条数，限制后台列表最大返回量。 */
  protected long normalizeSize(long size) {
    return Math.min(200, Math.max(1, size));
  }

  /** 判断字符串是否包含有效文本。 */
  protected boolean hasText(String value) {
    return StringUtils.hasText(value);
  }

  /** 将链、币种、状态等枚举型编码统一转成大写，避免查询和保存口径漂移。 */
  protected String normalizeFilterCode(String value) {
    return hasText(value) ? value.trim().toUpperCase() : null;
  }

  /** 统一清理自由文本字段两侧空白。 */
  protected String trimTextToNull(String value) {
    return hasText(value) ? value.trim() : null;
  }

  /** 校验查询结果必须存在，不存在时抛出业务友好的异常。 */
  protected <T> T require(T value, String message) {
    if (value == null) {
      throw new IllegalArgumentException(message);
    }
    return value;
  }

  /** 统一维护创建/更新时间并执行新增或更新。 */
  protected void touchAndSave(
      Long id,
      Consumer<LocalDateTime> setCreatedAt,
      Consumer<LocalDateTime> setUpdatedAt,
      Runnable insert,
      Runnable update) {
    LocalDateTime now = LocalDateTime.now();
    setUpdatedAt.accept(now);
    if (id == null) {
      setCreatedAt.accept(now);
      insert.run();
    } else {
      update.run();
    }
  }

  /** 按当前登录人的商户数据权限追加查询条件；ADMIN 不受限制。 */
  protected <T> void applyMerchantDataScope(
      LambdaQueryWrapper<T> query, SFunction<T, String> merchantGetter, String requestedMerchantId) {
    ManagerPrincipal principal = ManagerSecurityContext.requireCurrent();
    if (principal.admin() || hasAllDataScope(principal)) {
      return;
    }
    Set<String> allowedMerchantIds = merchantScopes(principal);
    if (hasText(requestedMerchantId)) {
      if (!containsIgnoreCase(allowedMerchantIds, requestedMerchantId)) {
        throw new IllegalArgumentException("merchant data access denied");
      }
      query.eq(merchantGetter, requestedMerchantId);
      return;
    }
    if (allowedMerchantIds.isEmpty()) {
      query.eq(merchantGetter, "__NO_MERCHANT_SCOPE__");
      return;
    }
    query.in(merchantGetter, allowedMerchantIds);
  }

  /** 校验当前登录人是否可访问指定商户数据；ADMIN 不受限制。 */
  protected void requireMerchantDataAccess(String merchantId) {
    ManagerPrincipal principal = ManagerSecurityContext.requireCurrent();
    if (principal.admin() || hasAllDataScope(principal)) {
      return;
    }
    if (!containsIgnoreCase(merchantScopes(principal), merchantId)) {
      throw new IllegalArgumentException("merchant data access denied");
    }
  }

  /** 按业务订单号查询关联回调记录。 */
  protected List<PaymentCallbackDeliveryRecord> callbacksByBizOrderNo(
      PaymentCallbackDeliveryRecordMapper callbackMapper, String bizOrderNo) {
    return callbackMapper.selectList(Wrappers.<PaymentCallbackDeliveryRecord>lambdaQuery()
        .eq(PaymentCallbackDeliveryRecord::getCryptoOrderNo, bizOrderNo)
        .orderByDesc(PaymentCallbackDeliveryRecord::getCreatedAt));
  }

  /** 按业务类型和业务键查询最近的审计记录。 */
  protected List<PaymentAuditRecord> audits(
      PaymentAuditRecordMapper auditMapper, String bizType, String bizKey) {
    return auditMapper.selectList(Wrappers.<PaymentAuditRecord>lambdaQuery()
        .eq(hasText(bizType), PaymentAuditRecord::getBizType, bizType)
        .eq(hasText(bizKey), PaymentAuditRecord::getBizKey, bizKey)
        .orderByDesc(PaymentAuditRecord::getCreatedAt)
        .last("LIMIT 100"));
  }

  private boolean hasAllDataScope(ManagerPrincipal principal) {
    return principal.dataScopes().stream()
        .anyMatch(scope -> "ALL".equalsIgnoreCase(scope.scopeType()));
  }

  private Set<String> merchantScopes(ManagerPrincipal principal) {
    Set<String> scopes = new LinkedHashSet<>();
    for (ManagerDataScope scope : principal.dataScopes()) {
      if ("MERCHANT".equalsIgnoreCase(scope.scopeType()) && hasText(scope.scopeValue())) {
        scopes.add(scope.scopeValue());
      }
    }
    return scopes;
  }

  private boolean containsIgnoreCase(Set<String> values, String expected) {
    if (!hasText(expected)) {
      return false;
    }
    return values.stream().anyMatch(value -> expected.equalsIgnoreCase(value));
  }
}
