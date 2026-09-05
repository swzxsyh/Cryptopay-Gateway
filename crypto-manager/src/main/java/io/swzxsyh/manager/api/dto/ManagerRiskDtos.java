package io.swzxsyh.manager.api.dto;

/** KYT 风控规则管理相关 DTO 聚合。 */
public final class ManagerRiskDtos {

  private ManagerRiskDtos() {}

  /** KYT 地址规则保存请求。 */
  public record AddressRuleRequest(Long id, String address, String ruleType, Boolean enabled) {}

  /** KYT 链规则保存请求。 */
  public record ChainRuleRequest(Long id, String chainCode, String ruleType, Boolean enabled) {}

  /** KYT Token 规则保存请求。 */
  public record TokenRuleRequest(
      Long id, String chainCode, String tokenSymbol, String ruleType, Boolean enabled) {}
}
