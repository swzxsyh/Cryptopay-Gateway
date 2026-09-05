package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerManualActionRequest;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerSubscriptionDtos.BillConfirmRequest;
import io.swzxsyh.manager.api.dto.ManagerSubscriptionDtos.DetailResponse;
import io.swzxsyh.manager.application.ManagerSubscriptionApplicationService;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/subscriptions")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SUBSCRIPTION_MANAGE)")
public class ManagerSubscriptionController {

  private final ManagerSubscriptionApplicationService subscriptionService;

  public ManagerSubscriptionController(ManagerSubscriptionApplicationService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  @GetMapping("/orders")
  /** 分页查询订阅订单。 */
  public ManagerApiResponse<ManagerPageResponse<SubscriptionOrder>> orders(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String subscriptionOrderNo,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        subscriptionService.pageSubscriptionOrders(page, size, merchantId, subscriptionOrderNo, status));
  }

  @GetMapping("/orders/{subscriptionOrderNo}")
  /** 查询订阅订单详情。 */
  public ManagerApiResponse<DetailResponse> orderDetail(
      @PathVariable String subscriptionOrderNo) {
    return ManagerApiResponse.ok(subscriptionService.subscriptionDetail(subscriptionOrderNo));
  }

  @PostMapping("/orders/{subscriptionOrderNo}/pause")
  /** 手动暂停订阅。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SUBSCRIPTION_ORDER_OPERATE)")
  public ManagerApiResponse<Object> pause(@PathVariable String subscriptionOrderNo) {
    return ManagerApiResponse.ok(subscriptionService.pauseSubscription(subscriptionOrderNo));
  }

  @PostMapping("/orders/{subscriptionOrderNo}/resume")
  /** 手动恢复订阅。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SUBSCRIPTION_ORDER_OPERATE)")
  public ManagerApiResponse<Object> resume(@PathVariable String subscriptionOrderNo) {
    return ManagerApiResponse.ok(subscriptionService.resumeSubscription(subscriptionOrderNo));
  }

  @PostMapping("/orders/{subscriptionOrderNo}/cancel")
  /** 手动取消订阅。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SUBSCRIPTION_ORDER_OPERATE)")
  public ManagerApiResponse<Object> cancel(@PathVariable String subscriptionOrderNo) {
    return ManagerApiResponse.ok(subscriptionService.cancelSubscription(subscriptionOrderNo));
  }

  @GetMapping("/billings")
  /** 分页查询订阅账单。 */
  public ManagerApiResponse<ManagerPageResponse<SubscriptionBillingRecord>> billings(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String subscriptionOrderNo,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        subscriptionService.pageSubscriptionBillings(page, size, merchantId, subscriptionOrderNo, status));
  }

  @PostMapping("/billings/{billingId}/confirm-paid")
  /** 手动确认订阅账单已支付。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SUBSCRIPTION_BILLING_OPERATE)")
  public ManagerApiResponse<SubscriptionBillingRecord> confirmBilling(
      @PathVariable Long billingId,
      @RequestBody(required = false) BillConfirmRequest request) {
    return ManagerApiResponse.ok(subscriptionService.confirmSubscriptionBilling(billingId, request));
  }

  @PostMapping("/billings/{billingId}/cancel")
  /** 手动关闭订阅账单。 */
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).SUBSCRIPTION_BILLING_OPERATE)")
  public ManagerApiResponse<SubscriptionBillingRecord> cancelBilling(
      @PathVariable Long billingId,
      @RequestBody(required = false) ManagerManualActionRequest request) {
    String reason = request == null ? "manager manual cancel" : request.reason();
    return ManagerApiResponse.ok(subscriptionService.cancelSubscriptionBilling(billingId, reason));
  }
}
