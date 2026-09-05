package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerOrderDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerPaymentOrderView;
import io.swzxsyh.manager.application.ManagerOrderApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/orders")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).ORDER_VIEW)")
public class ManagerOrderController {

  private final ManagerOrderApplicationService orderService;

  public ManagerOrderController(ManagerOrderApplicationService orderService) {
    this.orderService = orderService;
  }

  /** 分页查询普通支付订单列表。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<ManagerPaymentOrderView>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String merchantOrderNo,
      @RequestParam(required = false) String cryptoOrderNo,
      @RequestParam(required = false) String chain,
      @RequestParam(required = false) String token,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        orderService.pageOrders(
            page, size, merchantId, merchantOrderNo, cryptoOrderNo, chain, token, status));
  }

  /** 查询普通支付订单详情。 */
  @GetMapping("/{cryptoOrderNo}")
  public ManagerApiResponse<ManagerOrderDetailResponse> detail(
      @PathVariable String cryptoOrderNo) {
    return ManagerApiResponse.ok(orderService.orderDetail(cryptoOrderNo));
  }
}
