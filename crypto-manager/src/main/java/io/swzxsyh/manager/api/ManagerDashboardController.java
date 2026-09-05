package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerDashboardOverviewResponse;
import io.swzxsyh.manager.application.ManagerDashboardApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/dashboard")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).DASHBOARD_VIEW)")
public class ManagerDashboardController {

  private final ManagerDashboardApplicationService dashboardService;

  public ManagerDashboardController(ManagerDashboardApplicationService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /** 查询管理端首页概览数据。 */
  @GetMapping("/overview")
  public ManagerApiResponse<ManagerDashboardOverviewResponse> overview() {
    return ManagerApiResponse.ok(dashboardService.overview());
  }
}
