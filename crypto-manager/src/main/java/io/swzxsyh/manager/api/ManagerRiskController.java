package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.AddressRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.ChainRuleRequest;
import io.swzxsyh.manager.api.dto.ManagerRiskDtos.TokenRuleRequest;
import io.swzxsyh.manager.application.ManagerRiskApplicationService;
import io.swzxsyh.payment.persistence.entity.PaymentKytAddressRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytChainRule;
import io.swzxsyh.payment.persistence.entity.PaymentKytTokenRule;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/risk")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_MANAGE)")
public class ManagerRiskController {

  private final ManagerRiskApplicationService riskService;

  public ManagerRiskController(ManagerRiskApplicationService riskService) {
    this.riskService = riskService;
  }

  @GetMapping("/kyt/address-rules")
  /** 分页查询 KYT 地址规则。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentKytAddressRule>> addressRules(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String address,
      @RequestParam(required = false) String ruleType,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(
        riskService.pageAddressRules(page, size, address, ruleType, enabled));
  }

  @PostMapping("/kyt/address-rules")
  /** 新增或更新 KYT 地址规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_SAVE)")
  public ManagerApiResponse<PaymentKytAddressRule> saveAddressRule(
      @RequestBody AddressRuleRequest request) {
    return ManagerApiResponse.ok(riskService.saveAddressRule(request));
  }

  @DeleteMapping("/kyt/address-rules/{id}")
  /** 删除 KYT 地址规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_DELETE)")
  public ManagerApiResponse<Boolean> deleteAddressRule(@PathVariable Long id) {
    return ManagerApiResponse.ok(riskService.deleteAddressRule(id));
  }

  @GetMapping("/kyt/chain-rules")
  /** 分页查询 KYT 链规则。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentKytChainRule>> chainRules(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String chainCode,
      @RequestParam(required = false) String ruleType,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(
        riskService.pageChainRules(page, size, chainCode, ruleType, enabled));
  }

  @PostMapping("/kyt/chain-rules")
  /** 新增或更新 KYT 链规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_SAVE)")
  public ManagerApiResponse<PaymentKytChainRule> saveChainRule(
      @RequestBody ChainRuleRequest request) {
    return ManagerApiResponse.ok(riskService.saveChainRule(request));
  }

  @DeleteMapping("/kyt/chain-rules/{id}")
  /** 删除 KYT 链规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_DELETE)")
  public ManagerApiResponse<Boolean> deleteChainRule(@PathVariable Long id) {
    return ManagerApiResponse.ok(riskService.deleteChainRule(id));
  }

  @GetMapping("/kyt/token-rules")
  /** 分页查询 KYT Token 规则。 */
  public ManagerApiResponse<ManagerPageResponse<PaymentKytTokenRule>> tokenRules(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String chainCode,
      @RequestParam(required = false) String tokenSymbol,
      @RequestParam(required = false) String ruleType,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(
        riskService.pageTokenRules(page, size, chainCode, tokenSymbol, ruleType, enabled));
  }

  @PostMapping("/kyt/token-rules")
  /** 新增或更新 KYT Token 规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_SAVE)")
  public ManagerApiResponse<PaymentKytTokenRule> saveTokenRule(
      @RequestBody TokenRuleRequest request) {
    return ManagerApiResponse.ok(riskService.saveTokenRule(request));
  }

  @DeleteMapping("/kyt/token-rules/{id}")
  /** 删除 KYT Token 规则。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).RISK_RULE_DELETE)")
  public ManagerApiResponse<Boolean> deleteTokenRule(@PathVariable Long id) {
    return ManagerApiResponse.ok(riskService.deleteTokenRule(id));
  }
}
