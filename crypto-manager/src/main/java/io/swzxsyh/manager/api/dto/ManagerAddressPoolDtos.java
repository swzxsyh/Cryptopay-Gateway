package io.swzxsyh.manager.api.dto;

/** 地址池管理相关 DTO 聚合。 */
public final class ManagerAddressPoolDtos {

  private ManagerAddressPoolDtos() {}

  /** 地址池概览响应。 */
  public record OverviewResponse(
      String poolKey,
      String chain,
      String token,
      long availableCount,
      long leasedCount,
      long riskBlockedCount,
      long retiredCount,
      long redisAvailableCount,
      long redisLeasedCount) {}

  /** 地址池补池策略保存请求。 */
  public record PolicyRequest(Long id, String poolKey, Integer minSize, Boolean enabled) {}

  /** 手动冻结地址请求。 */
  public record BlockRequest(String reason, String providerId, Integer riskScore) {}
}
