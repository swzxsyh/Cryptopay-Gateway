package io.swzxsyh.manager.api.dto;

import java.time.LocalDateTime;

/** 对账管理相关 DTO 聚合。 */
public final class ManagerReconciliationDtos {

  private ManagerReconciliationDtos() {}

  /** 后台触发对账请求。 */
  public record RunRequest(LocalDateTime from, LocalDateTime to, Integer limit) {}

  /** 后台人工确认对账记录请求。 */
  public record HandleRequest(String operator, String operatorNote) {}
}
