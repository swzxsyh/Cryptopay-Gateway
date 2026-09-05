package io.swzxsyh.manager.api.dto;

import java.util.List;

/** 管理端列表筛选下拉选项 DTO。 */
public final class ManagerFilterOptionDtos {

  private ManagerFilterOptionDtos() {}

  /** 单个下拉选项，label 用于展示，value 用于传给查询接口。 */
  public record OptionItem(String label, String value) {}

  /** 管理端通用筛选选项集合。 */
  public record FilterOptionsResponse(
      List<OptionItem> merchants,
      List<OptionItem> chains,
      List<OptionItem> tokens,
      List<OptionItem> statuses,
      List<OptionItem> eventTypes) {}
}
