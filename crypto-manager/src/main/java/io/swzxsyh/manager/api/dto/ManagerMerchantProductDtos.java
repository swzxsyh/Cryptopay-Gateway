package io.swzxsyh.manager.api.dto;

import io.swzxsyh.payment.persistence.entity.MerchantProduct;
import java.time.LocalDateTime;

/** 商户产品库 DTO。 */
public final class ManagerMerchantProductDtos {

  private ManagerMerchantProductDtos() {}

  /** 产品库保存请求。 */
  public record ProductSaveRequest(
      Long id,
      String merchantId,
      String productName,
      String productUrl,
      String testUsername,
      String testPassword,
      String remark) {}

  /** 产品库列表与详情视图。 */
  public record ProductView(
      Long id,
      String merchantId,
      String productName,
      String productUrl,
      String testUsername,
      String testPassword,
      String remark,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static ProductView from(MerchantProduct product) {
      return new ProductView(
          product.getId(),
          product.getMerchantId(),
          product.getProductName(),
          product.getProductUrl(),
          product.getTestUsername(),
          product.getTestPassword(),
          product.getRemark(),
          product.getCreatedAt(),
          product.getUpdatedAt());
    }
  }
}
