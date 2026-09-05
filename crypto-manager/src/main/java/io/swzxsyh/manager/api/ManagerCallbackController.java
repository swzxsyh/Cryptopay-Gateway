package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerCallbackDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerCallbackApplicationService;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/callbacks")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CALLBACK_MANAGE)")
public class ManagerCallbackController {

  private final ManagerCallbackApplicationService callbackService;

  public ManagerCallbackController(ManagerCallbackApplicationService callbackService) {
    this.callbackService = callbackService;
  }

  /** 分页查询回调投递记录。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<PaymentCallbackDeliveryRecord>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String bizOrderNo,
      @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        callbackService.pageCallbacks(page, size, merchantId, bizOrderNo, eventType, status));
  }

  /** 查询单条回调投递详情。 */
  @GetMapping("/{id}")
  public ManagerApiResponse<ManagerCallbackDetailResponse> detail(@PathVariable Long id) {
    return ManagerApiResponse.ok(callbackService.callbackDetail(id));
  }

  /** 手动补发指定回调。 */
  @PostMapping("/{id}/replay")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).CALLBACK_REPLAY)")
  public ManagerApiResponse<Void> replay(@PathVariable Long id) {
    callbackService.replayCallback(id);
    return ManagerApiResponse.ok(null);
  }
}
