package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerFilterOptionDtos.FilterOptionsResponse;
import io.swzxsyh.manager.application.ManagerFilterOptionApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理端筛选下拉选项接口。 */
@RestController
@RequestMapping("/manager/options")
@PreAuthorize("isAuthenticated()")
public class ManagerFilterOptionController {

  private final ManagerFilterOptionApplicationService optionService;

  public ManagerFilterOptionController(ManagerFilterOptionApplicationService optionService) {
    this.optionService = optionService;
  }

  /** 返回商户号、链、代币、状态、事件等列表筛选选项。 */
  @GetMapping("/filters")
  public ManagerApiResponse<FilterOptionsResponse> filters() {
    return ManagerApiResponse.ok(optionService.filterOptions());
  }
}
