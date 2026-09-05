package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantProductDtos.ProductView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.payment.mapper.MerchantInfoMapper;
import io.swzxsyh.payment.mapper.MerchantProductMapper;
import io.swzxsyh.payment.persistence.entity.MerchantInfo;
import io.swzxsyh.payment.persistence.entity.MerchantProduct;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商户产品库应用服务，只用于后台记录商户产品和测试入口，不参与支付执行链路。 */
@Service
public class ManagerMerchantProductApplicationService extends ManagerApplicationSupport {

  private final MerchantProductMapper productMapper;
  private final MerchantInfoMapper merchantInfoMapper;

  public ManagerMerchantProductApplicationService(
      MerchantProductMapper productMapper,
      MerchantInfoMapper merchantInfoMapper) {
    this.productMapper = productMapper;
    this.merchantInfoMapper = merchantInfoMapper;
  }

  /** 分页查询产品库，支持按商户、产品名和链接模糊搜索。 */
  public ManagerPageResponse<ProductView> pageProducts(
      long page, long size, String merchantId, String productName, String productUrl) {
    LambdaQueryWrapper<MerchantProduct> query = Wrappers.<MerchantProduct>lambdaQuery()
        .eq(hasText(merchantId), MerchantProduct::getMerchantId, trim(merchantId))
        .like(hasText(productName), MerchantProduct::getProductName, trim(productName))
        .like(hasText(productUrl), MerchantProduct::getProductUrl, trim(productUrl))
        .orderByDesc(MerchantProduct::getUpdatedAt)
        .orderByDesc(MerchantProduct::getId);
    applyMerchantDataScope(query, MerchantProduct::getMerchantId, trim(merchantId));
    Page<MerchantProduct> result =
        productMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(ProductView::from).toList());
  }

  /** 新增或更新产品记录；商户号来自已存在商户，编辑时不允许修改归属商户。 */
  @Transactional(rollbackFor = Exception.class)
  public ProductView save(ProductSaveRequest request) {
    if (request == null || !hasText(request.merchantId()) || !hasText(request.productName())) {
      throw new IllegalArgumentException("merchantId and productName are required");
    }
    String merchantId = trim(request.merchantId());
    requireMerchantDataAccess(merchantId);
    require(findMerchant(merchantId), "merchant not found");

    MerchantProduct existing = request.id() == null ? null : productMapper.selectById(request.id());
    MerchantProduct product = existing == null ? new MerchantProduct() : existing;
    if (existing != null && !merchantId.equalsIgnoreCase(existing.getMerchantId())) {
      throw new IllegalArgumentException("merchantId cannot be changed after creation");
    }
    LocalDateTime now = LocalDateTime.now();
    product.setMerchantId(merchantId);
    product.setProductName(trim(request.productName()));
    product.setProductUrl(trimToNull(request.productUrl()));
    product.setTestUsername(trimToNull(request.testUsername()));
    product.setTestPassword(trimToNull(request.testPassword()));
    product.setRemark(trimToNull(request.remark()));
    product.setUpdatedAt(now);
    if (product.getId() == null) {
      product.setCreatedAt(now);
      productMapper.insert(product);
    } else {
      productMapper.updateById(product);
    }
    return ProductView.from(product);
  }

  private MerchantInfo findMerchant(String merchantId) {
    return merchantInfoMapper.selectOne(Wrappers.<MerchantInfo>lambdaQuery()
        .eq(MerchantInfo::getMerchantId, merchantId)
        .last("limit 1"));
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }

  private String trimToNull(String value) {
    return hasText(value) ? value.trim() : null;
  }
}
