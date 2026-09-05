package io.swzxsyh.manager.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelBatchSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.PaymentChannelView;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantSaveRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.MerchantView;
import io.swzxsyh.manager.api.dto.ManagerPageResponse;
import io.swzxsyh.manager.util.SnowflakeIdUtil;
import io.swzxsyh.payment.mapper.MerchantInfoMapper;
import io.swzxsyh.payment.mapper.MerchantPaymentChannelConfigMapper;
import io.swzxsyh.payment.mapper.PaymentTokenConfigMapper;
import io.swzxsyh.payment.persistence.entity.MerchantInfo;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import io.swzxsyh.payment.persistence.entity.PaymentTokenConfig;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 商户主数据应用服务；只维护商户身份，不处理商户密钥、费率等独立领域配置。 */
@Service
public class ManagerMerchantApplicationService extends ManagerApplicationSupport {

  private final MerchantInfoMapper merchantInfoMapper;
  private final MerchantPaymentChannelConfigMapper channelConfigMapper;
  private final PaymentTokenConfigMapper tokenConfigMapper;
  private final SnowflakeIdUtil snowflakeIdUtil;

  public ManagerMerchantApplicationService(
      MerchantInfoMapper merchantInfoMapper,
      MerchantPaymentChannelConfigMapper channelConfigMapper,
      PaymentTokenConfigMapper tokenConfigMapper,
      SnowflakeIdUtil snowflakeIdUtil) {
    this.merchantInfoMapper = merchantInfoMapper;
    this.channelConfigMapper = channelConfigMapper;
    this.tokenConfigMapper = tokenConfigMapper;
    this.snowflakeIdUtil = snowflakeIdUtil;
  }

  /** 分页查询商户主数据，普通运营账号会按商户数据权限收敛可见范围。 */
  public ManagerPageResponse<MerchantView> pageMerchants(
      long page, long size, String merchantId, String status) {
    LambdaQueryWrapper<MerchantInfo> query = Wrappers.<MerchantInfo>lambdaQuery()
        .like(hasText(merchantId), MerchantInfo::getMerchantId, merchantId)
        .eq(hasText(status), MerchantInfo::getStatus, normalizeStatus(status))
        .orderByDesc(MerchantInfo::getCreatedAt)
        .orderByAsc(MerchantInfo::getId);
    applyMerchantDataScope(query, MerchantInfo::getMerchantId, merchantId);
    Page<MerchantInfo> result = merchantInfoMapper.selectPage(
        new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(MerchantView::from).toList());
  }

  /** 查询商户详情。 */
  public MerchantView detail(String merchantId) {
    if (!hasText(merchantId)) {
      throw new IllegalArgumentException("merchantId is required");
    }
    requireMerchantDataAccess(merchantId);
    MerchantInfo merchant = merchantInfoMapper.selectOne(Wrappers.<MerchantInfo>lambdaQuery()
        .eq(MerchantInfo::getMerchantId, merchantId.trim())
        .last("limit 1"));
    return MerchantView.from(require(merchant, "merchant not found"));
  }

  /** 新增或更新商户主数据；新增后可被用户数据权限和各业务筛选下拉使用。 */
  @Transactional(rollbackFor = Exception.class)
  public MerchantView save(MerchantSaveRequest request) {
    if (request == null || !hasText(request.merchantName())) {
      throw new IllegalArgumentException("merchantName is required");
    }
    MerchantInfo existing = request.id() == null ? null : merchantInfoMapper.selectById(request.id());
    MerchantInfo merchant = existing == null ? new MerchantInfo() : existing;
    String merchantId = existing == null ? snowflakeIdUtil.nextMerchantId() : existing.getMerchantId();
    if (existing != null && hasText(request.merchantId())
        && !request.merchantId().trim().equalsIgnoreCase(existing.getMerchantId())) {
      throw new IllegalArgumentException("merchantId cannot be changed after creation");
    }
    LocalDateTime now = LocalDateTime.now();
    merchant.setMerchantId(merchantId);
    merchant.setMerchantName(request.merchantName().trim());
    merchant.setStatus(normalizeStatus(request.status()));
    merchant.setContactEmail(trimToNull(request.contactEmail()));
    merchant.setContactPhone(trimToNull(request.contactPhone()));
    merchant.setDefaultNotifyUrl(trimToNull(request.defaultNotifyUrl()));
    merchant.setDefaultReturnUrl(trimToNull(request.defaultReturnUrl()));
    merchant.setDefaultWithdrawChain(trimToNull(request.defaultWithdrawChain()));
    merchant.setDefaultWithdrawAddress(trimToNull(request.defaultWithdrawAddress()));
    merchant.setRemark(trimToNull(request.remark()));
    merchant.setUpdatedAt(now);
    if (merchant.getId() == null) {
      merchant.setCreatedAt(now);
      insertNewMerchantWithGeneratedId(merchant);
    } else {
      merchantInfoMapper.updateById(merchant);
    }
    return MerchantView.from(merchant);
  }

  private void insertNewMerchantWithGeneratedId(MerchantInfo merchant) {
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        merchantInfoMapper.insert(merchant);
        return;
      } catch (DuplicateKeyException ex) {
        if (attempt >= 3) {
          throw ex;
        }
        merchant.setMerchantId(snowflakeIdUtil.nextMerchantId());
      }
    }
  }

  /** 分页查询商户支付通道和费率配置。 */
  public ManagerPageResponse<PaymentChannelView> pagePaymentChannels(
      long page, long size, String merchantId, String chainCode, String tokenSymbol, Boolean enabled) {
    LambdaQueryWrapper<MerchantPaymentChannelConfig> query =
        Wrappers.<MerchantPaymentChannelConfig>lambdaQuery()
            .eq(hasText(chainCode), MerchantPaymentChannelConfig::getChainCode, normalizeCode(chainCode))
            .eq(hasText(tokenSymbol), MerchantPaymentChannelConfig::getTokenSymbol, normalizeCode(tokenSymbol))
            .eq(enabled != null, MerchantPaymentChannelConfig::getEnabled, enabled)
            .orderByAsc(MerchantPaymentChannelConfig::getMerchantId)
            .orderByAsc(MerchantPaymentChannelConfig::getChainCode)
            .orderByAsc(MerchantPaymentChannelConfig::getTokenSymbol);
    applyMerchantDataScope(query, MerchantPaymentChannelConfig::getMerchantId, merchantId);
    Page<MerchantPaymentChannelConfig> result =
        channelConfigMapper.selectPage(new Page<>(normalizePage(page), normalizeSize(size)), query);
    return new ManagerPageResponse<>(
        result.getCurrent(),
        result.getSize(),
        result.getTotal(),
        result.getRecords().stream().map(PaymentChannelView::from).toList());
  }

  /** 保存单条商户支付通道费率配置；链币组合一旦创建后只允许改费率、限额和开关。 */
  @Transactional(rollbackFor = Exception.class)
  public PaymentChannelView savePaymentChannel(PaymentChannelSaveRequest request) {
    MerchantPaymentChannelConfig config = savePaymentChannelInternal(
        request.merchantId(),
        request.chainCode(),
        request.tokenSymbol(),
        request.enabled(),
        request.transactionFeeRate(),
        request.minimumFee(),
        request.fixedFee(),
        request.gatewayFee(),
        request.taxRate(),
        request.minOrderAmount(),
        request.maxOrderAmount(),
        request.remark(),
        request.id());
    return PaymentChannelView.from(config);
  }

  /** 批量开通商户支付通道，适配前端勾选多个链币后一次性提交。 */
  @Transactional(rollbackFor = Exception.class)
  public List<PaymentChannelView> savePaymentChannels(PaymentChannelBatchSaveRequest request) {
    if (request == null || !hasText(request.merchantId()) || request.selections() == null
        || request.selections().isEmpty()) {
      throw new IllegalArgumentException("merchantId and selections are required");
    }
    return request.selections().stream()
        .map(selection -> savePaymentChannelInternal(
            request.merchantId(),
            selection.chainCode(),
            selection.tokenSymbol(),
            request.enabled(),
            request.transactionFeeRate(),
            request.minimumFee(),
            request.fixedFee(),
            request.gatewayFee(),
            request.taxRate(),
            request.minOrderAmount(),
            request.maxOrderAmount(),
            request.remark(),
            null))
        .map(PaymentChannelView::from)
        .toList();
  }

  private MerchantPaymentChannelConfig savePaymentChannelInternal(
      String merchantId,
      String chainCode,
      String tokenSymbol,
      Boolean enabled,
      BigDecimal transactionFeeRate,
      BigDecimal minimumFee,
      BigDecimal fixedFee,
      BigDecimal gatewayFee,
      BigDecimal taxRate,
      BigDecimal minOrderAmount,
      BigDecimal maxOrderAmount,
      String remark,
      Long id) {
    if (!hasText(merchantId) || !hasText(chainCode) || !hasText(tokenSymbol)) {
      throw new IllegalArgumentException("merchantId, chainCode and tokenSymbol are required");
    }
    String normalizedMerchantId = merchantId.trim();
    String normalizedChain = normalizeCode(chainCode);
    String normalizedToken = normalizeCode(tokenSymbol);
    requireMerchantDataAccess(normalizedMerchantId);
    require(findByMerchantId(normalizedMerchantId), "merchant not found");
    requireTokenConfig(normalizedChain, normalizedToken);
    validateAmountRange(minOrderAmount, maxOrderAmount);

    MerchantPaymentChannelConfig existing = id == null
        ? findPaymentChannel(normalizedMerchantId, normalizedChain, normalizedToken)
        : channelConfigMapper.selectById(id);
    MerchantPaymentChannelConfig config = existing == null ? new MerchantPaymentChannelConfig() : existing;
    if (existing != null && (!normalizedMerchantId.equalsIgnoreCase(existing.getMerchantId())
        || !normalizedChain.equalsIgnoreCase(existing.getChainCode())
        || !normalizedToken.equalsIgnoreCase(existing.getTokenSymbol()))) {
      throw new IllegalArgumentException("merchant payment channel identity cannot be changed");
    }
    LocalDateTime now = LocalDateTime.now();
    config.setMerchantId(normalizedMerchantId);
    config.setChainCode(normalizedChain);
    config.setTokenSymbol(normalizedToken);
    config.setEnabled(enabled == null || Boolean.TRUE.equals(enabled));
    config.setTransactionFeeRate(defaultZero(transactionFeeRate));
    config.setMinimumFee(defaultZero(minimumFee));
    config.setFixedFee(defaultZero(fixedFee));
    config.setGatewayFee(defaultZero(gatewayFee));
    config.setTaxRate(defaultZero(taxRate));
    config.setMinOrderAmount(defaultZero(minOrderAmount));
    config.setMaxOrderAmount(defaultZero(maxOrderAmount));
    config.setRemark(trimToNull(remark));
    config.setUpdatedAt(now);
    if (config.getId() == null) {
      config.setCreatedAt(now);
      channelConfigMapper.insert(config);
    } else {
      channelConfigMapper.updateById(config);
    }
    return config;
  }

  private MerchantPaymentChannelConfig findPaymentChannel(String merchantId, String chainCode, String tokenSymbol) {
    return channelConfigMapper.selectOne(Wrappers.<MerchantPaymentChannelConfig>lambdaQuery()
        .eq(MerchantPaymentChannelConfig::getMerchantId, merchantId)
        .eq(MerchantPaymentChannelConfig::getChainCode, chainCode)
        .eq(MerchantPaymentChannelConfig::getTokenSymbol, tokenSymbol)
        .last("limit 1"));
  }

  private void requireTokenConfig(String chainCode, String tokenSymbol) {
    PaymentTokenConfig tokenConfig = tokenConfigMapper.selectOne(Wrappers.<PaymentTokenConfig>lambdaQuery()
        .eq(PaymentTokenConfig::getChainCode, chainCode)
        .eq(PaymentTokenConfig::getTokenSymbol, tokenSymbol)
        .last("limit 1"));
    require(tokenConfig, "payment token config not found");
  }

  private void validateAmountRange(BigDecimal minOrderAmount, BigDecimal maxOrderAmount) {
    if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("minOrderAmount cannot be negative");
    }
    if (maxOrderAmount != null && maxOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("maxOrderAmount cannot be negative");
    }
    if (minOrderAmount != null && maxOrderAmount != null
        && maxOrderAmount.compareTo(BigDecimal.ZERO) > 0
        && minOrderAmount.compareTo(maxOrderAmount) > 0) {
      throw new IllegalArgumentException("minOrderAmount cannot be greater than maxOrderAmount");
    }
  }

  private BigDecimal defaultZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private MerchantInfo findByMerchantId(String merchantId) {
    return merchantInfoMapper.selectOne(Wrappers.<MerchantInfo>lambdaQuery()
        .eq(MerchantInfo::getMerchantId, merchantId)
        .last("limit 1"));
  }

  private String normalizeStatus(String status) {
    return hasText(status) ? status.trim().toUpperCase() : "ENABLED";
  }

  private String normalizeCode(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    return hasText(value) ? value.trim() : null;
  }
}
