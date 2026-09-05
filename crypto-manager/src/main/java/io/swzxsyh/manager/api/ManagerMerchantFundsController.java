package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAccountView;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustResponse;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.WithdrawHandleRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.WithdrawRecordView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerMerchantFundsApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 商户资金余额和提现审核处理接口；提现申请入口不在 manager 端。 */
@RestController
@RequestMapping("/manager/merchant-funds")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_BALANCE_MANAGE)")
public class ManagerMerchantFundsController {

  private final ManagerMerchantFundsApplicationService fundsService;

  public ManagerMerchantFundsController(ManagerMerchantFundsApplicationService fundsService) {
    this.fundsService = fundsService;
  }

  /** 分页查询商户资金余额。 */
  @GetMapping("/balances")
  public ManagerApiResponse<ManagerPageResponse<BalanceAccountView>> balances(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String token) {
    return ManagerApiResponse.ok(fundsService.pageBalances(page, size, merchantId, token));
  }

  /** 分页查询提现流水。 */
  @GetMapping("/withdrawals")
  public ManagerApiResponse<ManagerPageResponse<WithdrawRecordView>> withdrawals(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String token,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(fundsService.pageWithdrawals(page, size, merchantId, token, status));
  }

  /** 人工调账商户余额，提现申请与处理后续由 merchant 模块承载。 */
  @PostMapping("/balances/adjust")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_BALANCE_ADJUST)")
  public ManagerApiResponse<BalanceAdjustResponse> adjustBalance(
      @RequestBody BalanceAdjustRequest request) {
    return ManagerApiResponse.ok(fundsService.adjustBalance(request));
  }

  /** 审核通过提现申请，并冻结商户余额。 */
  @PostMapping("/withdrawals/{id}/approve")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_WITHDRAW_OPERATE)")
  public ManagerApiResponse<WithdrawRecordView> approveWithdraw(
      @PathVariable Long id, @RequestBody(required = false) WithdrawHandleRequest request) {
    return ManagerApiResponse.ok(fundsService.approveWithdraw(id, request));
  }

  /** 拒绝提现申请。 */
  @PostMapping("/withdrawals/{id}/reject")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_WITHDRAW_OPERATE)")
  public ManagerApiResponse<WithdrawRecordView> rejectWithdraw(
      @PathVariable Long id, @RequestBody(required = false) WithdrawHandleRequest request) {
    return ManagerApiResponse.ok(fundsService.rejectWithdraw(id, request));
  }

  /** 标记提现进入链上处理中。 */
  @PostMapping("/withdrawals/{id}/processing")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_WITHDRAW_OPERATE)")
  public ManagerApiResponse<WithdrawRecordView> markWithdrawProcessing(
      @PathVariable Long id, @RequestBody(required = false) WithdrawHandleRequest request) {
    return ManagerApiResponse.ok(fundsService.markWithdrawProcessing(id, request));
  }

  /** 标记提现成功。 */
  @PostMapping("/withdrawals/{id}/succeeded")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_WITHDRAW_OPERATE)")
  public ManagerApiResponse<WithdrawRecordView> markWithdrawSucceeded(
      @PathVariable Long id, @RequestBody(required = false) WithdrawHandleRequest request) {
    return ManagerApiResponse.ok(fundsService.markWithdrawSucceeded(id, request));
  }

  /** 标记提现失败并释放冻结余额。 */
  @PostMapping("/withdrawals/{id}/failed")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_WITHDRAW_OPERATE)")
  public ManagerApiResponse<WithdrawRecordView> markWithdrawFailed(
      @PathVariable Long id, @RequestBody(required = false) WithdrawHandleRequest request) {
    return ManagerApiResponse.ok(fundsService.markWithdrawFailed(id, request));
  }
}
