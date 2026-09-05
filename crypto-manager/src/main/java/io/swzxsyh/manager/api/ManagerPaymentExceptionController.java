package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.api.dto.ManagerPaymentExceptionDetailResponse;
import io.swzxsyh.manager.api.dto.ManagerPaymentExceptionHandleRequest;
import io.swzxsyh.manager.application.ManagerPaymentExceptionApplicationService;
import io.swzxsyh.payment.persistence.entity.PaymentExceptionOrder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端支付异常单接口。 */
@RestController
@RequestMapping("/manager/payment-exceptions")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).PAYMENT_EXCEPTION_MANAGE)")
public class ManagerPaymentExceptionController {

  private final ManagerPaymentExceptionApplicationService exceptionService;

  public ManagerPaymentExceptionController(ManagerPaymentExceptionApplicationService exceptionService) {
    this.exceptionService = exceptionService;
  }

  /** 分页查询支付异常单。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<PaymentExceptionOrder>> page(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String cryptoOrderNo,
      @RequestParam(required = false) String exceptionType,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(
        exceptionService.pagePaymentExceptions(
            page, size, merchantId, cryptoOrderNo, exceptionType, status));
  }

  /** 查询支付异常单详情。 */
  @GetMapping("/{id}")
  public ManagerApiResponse<ManagerPaymentExceptionDetailResponse> detail(@PathVariable Long id) {
    return ManagerApiResponse.ok(exceptionService.paymentExceptionDetail(id));
  }

  /** 人工处理支付异常单。 */
  @PostMapping("/{id}/handle")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).PAYMENT_EXCEPTION_HANDLE)")
  public ManagerApiResponse<PaymentExceptionOrder> handle(
      @PathVariable Long id, @RequestBody(required = false) ManagerPaymentExceptionHandleRequest request) {
    return ManagerApiResponse.ok(exceptionService.handlePaymentException(id, request));
  }
}
