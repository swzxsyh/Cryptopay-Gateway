package io.swzxsyh.manager.api;

import io.swzxsyh.manager.api.dto.ManagerApiResponse;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.application.ManagerMerchantProductApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 商户产品库管理接口。 */
@RestController
@RequestMapping("/manager/merchant-products")
@PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_PRODUCT_MANAGE)")
public class ManagerMerchantProductController {

  private final ManagerMerchantProductApplicationService productService;

  public ManagerMerchantProductController(ManagerMerchantProductApplicationService productService) {
    this.productService = productService;
  }

  /** 分页查询产品库记录。 */
  @GetMapping
  public ManagerApiResponse<ManagerPageResponse<ProductView>> products(
      @RequestParam(defaultValue = "1") long page,
      @RequestParam(defaultValue = "20") long size,
      @RequestParam(required = false) String merchantId,
      @RequestParam(required = false) String productName,
      @RequestParam(required = false) String productUrl) {
    return ManagerApiResponse.ok(
        productService.pageProducts(page, size, merchantId, productName, productUrl));
  }

  /** 新增或更新产品库记录。 */
  @PostMapping
  @PreAuthorize("@managerPermissionService.hasFunction(authentication, T(io.swzxsyh.manager.security.ManagerPermission).MERCHANT_PRODUCT_SAVE)")
  public ManagerApiResponse<ProductView> save(@RequestBody ProductSaveRequest request) {
    return ManagerApiResponse.ok(productService.save(request));
  }
}
