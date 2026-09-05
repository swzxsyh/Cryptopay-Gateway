package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantView;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelBatchSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerMerchantApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/** 商户主数据管理接口，不暴露商户私钥、公钥等安全材料配置。 */
@RestController
@RequestMapping("/manager/merchants")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_MANAGE)")
public class ManagerMerchantController {

  private final ManagerMerchantApplicationService merchantService;

  public ManagerMerchantController(ManagerMerchantApplicationService merchantService) {
    this.merchantService = merchantService;
  }

  /** 分页查询商户。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<MerchantView>> merchants(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String status) {
    return ManagerApiResponse.ok(merchantService.pageMerchants(page, size, merchantId, status));
  }

  /** 查询商户详情。 */
  @GetMapping("/{merchantId}")
  public ManagerApiResponse<MerchantView> detail(@PathVariable String merchantId) {
    return ManagerApiResponse.ok(merchantService.detail(merchantId));
  }

  /** 新增或更新商户主数据。 */
  @PostMapping
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_SAVE)")
  public ManagerApiResponse<MerchantView> save(@RequestBody MerchantSaveRequest request) {
    return ManagerApiResponse.ok(merchantService.save(request));
  }

  /** 分页查询商户可用链币和费率配置。 */
  @GetMapping("/payment-channels")
  public ManagerApiResponse<ManagerPageResponse<PaymentChannelView>> paymentChannels(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String chainCode,
      @RequestParam(required = false) String tokenSymbol,
      @RequestParam(required = false) Boolean enabled) {
    return ManagerApiResponse.ok(
        merchantService.pagePaymentChannels(page, size, merchantId, chainCode, tokenSymbol, enabled));
  }

  /** 保存单条商户通道费率。 */
  @PostMapping("/payment-channels")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_CHANNEL_SAVE)")
  public ManagerApiResponse<PaymentChannelView> savePaymentChannel(
      @RequestBody PaymentChannelSaveRequest request) {
    return ManagerApiResponse.ok(merchantService.savePaymentChannel(request));
  }

  /** 批量开通商户通道费率，失败时整体回滚。 */
  @PostMapping("/payment-channels/batch")
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_CHANNEL_SAVE)")
  public ManagerApiResponse<List<PaymentChannelView>> savePaymentChannels(
      @RequestBody PaymentChannelBatchSaveRequest request) {
    return ManagerApiResponse.ok(merchantService.savePaymentChannels(request));
  }
}
