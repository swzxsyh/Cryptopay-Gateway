package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.manager.api.dto.ManagerFilterOptionDtos.FilterOptionsResponse;
import io.swzxsyh.manager.api.dto.ManagerFilterOptionDtos.OptionItem;
import io.swzxsyh.manager.security.ManagerDataScope;
import io.swzxsyh.manager.security.ManagerPrincipal;
import io.swzxsyh.manager.security.ManagerSecurityContext;
import io.swzxsyh.payment.callback.CallbackDeliveryStatus;
import io.swzxsyh.payment.domain.ContractSettlementStatus;
import io.swzxsyh.payment.domain.OrderStatus;
import io.swzxsyh.payment.exceptionorder.PaymentExceptionStatus;
import io.swzxsyh.payment.mapper.ContractSettlementRecordMapper;
import io.swzxsyh.payment.mapper.DerivedAddressPoolRecordMapper;
import io.swzxsyh.payment.mapper.MerchantInfoMapper;
import io.swzxsyh.payment.mapper.PaymentAuditRecordMapper;
import io.swzxsyh.payment.mapper.PaymentCallbackDeliveryRecordMapper;
import io.swzxsyh.payment.mapper.PaymentChainConfigMapper;
import io.swzxsyh.payment.mapper.PaymentExceptionOrderMapper;
import io.swzxsyh.payment.mapper.PaymentOrderMapper;
import io.swzxsyh.payment.mapper.PaymentReconciliationRecordMapper;
import io.swzxsyh.payment.mapper.PaymentTokenConfigMapper;
import io.swzxsyh.payment.mapper.RawChainLogMapper;
import io.swzxsyh.payment.mapper.SubscriptionBillingRecordMapper;
import io.swzxsyh.payment.mapper.SubscriptionOrderMapper;
import io.swzxsyh.payment.persistence.entity.DerivedAddressPoolStatus;
import io.swzxsyh.payment.rawchain.RawChainLogStatus;
import io.swzxsyh.payment.reconciliation.ReconciliationStatus;
import io.swzxsyh.payment.subscription.SubscriptionBillingStatus;
import io.swzxsyh.payment.subscription.SubscriptionStatus;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 管理端列表筛选项聚合服务，统一从配置表和业务表提取可选值。 */
@Service
public class ManagerFilterOptionApplicationService {

  private static final int MAX_OPTION_SIZE = 500;

  private final PaymentOrderMapper orderMapper;
  private final MerchantInfoMapper merchantInfoMapper;
  private final PaymentCallbackDeliveryRecordMapper callbackMapper;
  private final PaymentAuditRecordMapper auditMapper;
  private final PaymentChainConfigMapper chainConfigMapper;
  private final PaymentTokenConfigMapper tokenConfigMapper;
  private final RawChainLogMapper rawChainLogMapper;
  private final PaymentExceptionOrderMapper exceptionOrderMapper;
  private final PaymentReconciliationRecordMapper reconciliationRecordMapper;
  private final ContractSettlementRecordMapper settlementRecordMapper;
  private final DerivedAddressPoolRecordMapper addressPoolRecordMapper;
  private final SubscriptionOrderMapper subscriptionOrderMapper;
  private final SubscriptionBillingRecordMapper subscriptionBillingRecordMapper;

  public ManagerFilterOptionApplicationService(
      PaymentOrderMapper orderMapper,
      MerchantInfoMapper merchantInfoMapper,
      PaymentCallbackDeliveryRecordMapper callbackMapper,
      PaymentAuditRecordMapper auditMapper,
      PaymentChainConfigMapper chainConfigMapper,
      PaymentTokenConfigMapper tokenConfigMapper,
      RawChainLogMapper rawChainLogMapper,
      PaymentExceptionOrderMapper exceptionOrderMapper,
      PaymentReconciliationRecordMapper reconciliationRecordMapper,
      ContractSettlementRecordMapper settlementRecordMapper,
      DerivedAddressPoolRecordMapper addressPoolRecordMapper,
      SubscriptionOrderMapper subscriptionOrderMapper,
      SubscriptionBillingRecordMapper subscriptionBillingRecordMapper) {
    this.orderMapper = orderMapper;
    this.merchantInfoMapper = merchantInfoMapper;
    this.callbackMapper = callbackMapper;
    this.auditMapper = auditMapper;
    this.chainConfigMapper = chainConfigMapper;
    this.tokenConfigMapper = tokenConfigMapper;
    this.rawChainLogMapper = rawChainLogMapper;
    this.exceptionOrderMapper = exceptionOrderMapper;
    this.reconciliationRecordMapper = reconciliationRecordMapper;
    this.settlementRecordMapper = settlementRecordMapper;
    this.addressPoolRecordMapper = addressPoolRecordMapper;
    this.subscriptionOrderMapper = subscriptionOrderMapper;
    this.subscriptionBillingRecordMapper = subscriptionBillingRecordMapper;
  }

  /** 查询管理端列表页可复用的商户号、链、代币、状态和事件类型下拉选项。 */
  public FilterOptionsResponse filterOptions() {
    return new FilterOptionsResponse(
        optionItems(merchantValues()),
        optionItems(chainValues()),
        optionItems(tokenValues()),
        optionItems(statusValues()),
        optionItems(eventTypeValues()));
  }

  private Set<String> merchantValues() {
    ManagerPrincipal principal = ManagerSecurityContext.requireCurrent();
    if (!principal.admin() && !hasAllDataScope(principal)) {
      return merchantScopes(principal);
    }
    Set<String> values = new LinkedHashSet<>();
    addDistinct(values, merchantInfoMapper, "merchant_id");
    addDistinct(values, orderMapper, "merchant_id");
    addDistinct(values, callbackMapper, "merchant_id");
    addDistinct(values, auditMapper, "merchant_id");
    addDistinct(values, exceptionOrderMapper, "merchant_id");
    addDistinct(values, reconciliationRecordMapper, "merchant_id");
    addDistinct(values, subscriptionOrderMapper, "merchant_id");
    addDistinct(values, subscriptionBillingRecordMapper, "merchant_id");
    return values;
  }

  private Set<String> chainValues() {
    Set<String> values = new LinkedHashSet<>();
    addDistinct(values, chainConfigMapper, "chain_code");
    addDistinct(values, tokenConfigMapper, "chain_code");
    addDistinct(values, orderMapper, "chain");
    addDistinct(values, rawChainLogMapper, "chain");
    addDistinct(values, exceptionOrderMapper, "chain");
    addDistinct(values, reconciliationRecordMapper, "chain");
    addDistinct(values, settlementRecordMapper, "chain");
    addDistinct(values, addressPoolRecordMapper, "chain");
    addDistinct(values, subscriptionOrderMapper, "chain");
    addDistinct(values, subscriptionBillingRecordMapper, "chain");
    return values;
  }

  private Set<String> tokenValues() {
    Set<String> values = new LinkedHashSet<>();
    addDistinct(values, tokenConfigMapper, "token_symbol");
    addDistinct(values, orderMapper, "token");
    addDistinct(values, rawChainLogMapper, "token");
    addDistinct(values, exceptionOrderMapper, "token");
    addDistinct(values, reconciliationRecordMapper, "token");
    addDistinct(values, settlementRecordMapper, "token");
    addDistinct(values, addressPoolRecordMapper, "token");
    addDistinct(values, subscriptionOrderMapper, "token");
    addDistinct(values, subscriptionBillingRecordMapper, "token");
    return values;
  }

  private Set<String> statusValues() {
    Set<String> values = new LinkedHashSet<>();
    addEnumValues(values, OrderStatus.class);
    addEnumValues(values, CallbackDeliveryStatus.class);
    addEnumValues(values, RawChainLogStatus.class);
    addEnumValues(values, PaymentExceptionStatus.class);
    addEnumValues(values, ReconciliationStatus.class);
    addEnumValues(values, ContractSettlementStatus.class);
    addEnumValues(values, DerivedAddressPoolStatus.class);
    addEnumValues(values, SubscriptionStatus.class);
    addEnumValues(values, SubscriptionBillingStatus.class);
    addDistinct(values, orderMapper, "status");
    addDistinct(values, callbackMapper, "status");
    addDistinct(values, auditMapper, "status");
    addDistinct(values, rawChainLogMapper, "status");
    addDistinct(values, exceptionOrderMapper, "status");
    addDistinct(values, reconciliationRecordMapper, "reconcile_status");
    addDistinct(values, settlementRecordMapper, "status");
    addDistinct(values, addressPoolRecordMapper, "status");
    addDistinct(values, subscriptionOrderMapper, "status");
    addDistinct(values, subscriptionBillingRecordMapper, "status");
    return values;
  }

  private void addEnumValues(Set<String> target, Class<? extends Enum<?>> enumType) {
    for (Enum<?> item : enumType.getEnumConstants()) {
      addValue(target, item.name());
    }
  }

  private Set<String> eventTypeValues() {
    Set<String> values = new LinkedHashSet<>();
    addDistinct(values, callbackMapper, "event_type");
    addDistinct(values, auditMapper, "event_type");
    return values;
  }

  private <T> void addDistinct(Set<String> target, BaseMapper<T> mapper, String column) {
    if (target.size() >= MAX_OPTION_SIZE) {
      return;
    }
    QueryWrapper<T> query = new QueryWrapper<T>()
        .select("DISTINCT " + column)
        .isNotNull(column)
        .ne(column, "")
        .orderByAsc(column)
        .last("LIMIT " + MAX_OPTION_SIZE);
    for (Object value : mapper.selectObjs(query)) {
      addValue(target, value);
      if (target.size() >= MAX_OPTION_SIZE) {
        return;
      }
    }
  }

  private void addValue(Set<String> target, Object value) {
    if (value == null) {
      return;
    }
    String text = String.valueOf(value).trim();
    if (StringUtils.hasText(text)) {
      target.add(text);
    }
  }

  private List<OptionItem> optionItems(Set<String> values) {
    List<String> sorted = new ArrayList<>(values);
    sorted.sort(String.CASE_INSENSITIVE_ORDER);
    return sorted.stream().map(value -> new OptionItem(value, value)).toList();
  }

  private boolean hasAllDataScope(ManagerPrincipal principal) {
    return principal.dataScopes().stream()
        .anyMatch(scope -> "ALL".equalsIgnoreCase(scope.scopeType()));
  }

  private Set<String> merchantScopes(ManagerPrincipal principal) {
    Set<String> scopes = new LinkedHashSet<>();
    for (ManagerDataScope scope : principal.dataScopes()) {
      if ("MERCHANT".equalsIgnoreCase(scope.scopeType()) && StringUtils.hasText(scope.scopeValue())) {
        scopes.add(scope.scopeValue().trim());
      }
    }
    return scopes;
  }
}
