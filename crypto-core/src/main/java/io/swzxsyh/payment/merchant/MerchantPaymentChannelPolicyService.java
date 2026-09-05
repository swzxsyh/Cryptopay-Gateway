package io.swzxsyh.payment.merchant;

import io.swzxsyh.payment.mapper.MerchantPaymentChannelConfigMapper;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 商户链币开通策略，供收银台展示和支付方式选择时校验。 */
@Service
public class MerchantPaymentChannelPolicyService {

  private final MerchantPaymentChannelConfigMapper mapper;

  public MerchantPaymentChannelPolicyService(MerchantPaymentChannelConfigMapper mapper) {
    this.mapper = mapper;
  }

  /** 查询商户已启用的链币配置；没有配置时返回空列表，表示该商户未开通任何链币。 */
  public List<MerchantPaymentChannelConfig> enabledChannels(String merchantId, String tokenSymbol) {
    if (!StringUtils.hasText(merchantId)) {
      return List.of();
    }
    return mapper.selectEnabledUsableChannels(merchantId.trim(), normalize(tokenSymbol));
  }

  /** 校验商户至少存在一个可用链币通道，适用于下单时只确定币种、尚未选择链的阶段。 */
  public void requireAnyUsable(String merchantId, String tokenSymbol, BigDecimal amount) {
    if (!StringUtils.hasText(merchantId) || !StringUtils.hasText(tokenSymbol)) {
      throw new IllegalArgumentException("merchantId and tokenSymbol are required");
    }
    boolean usable = enabledChannels(merchantId, tokenSymbol).stream()
        .anyMatch(channel -> amountWithinLimit(channel, amount));
    if (!usable) {
      throw new IllegalArgumentException("Merchant has no enabled payment channel for token or amount");
    }
  }

  /** 校验商户是否允许使用指定链币、平台链币是否开启，以及订单金额是否在该通道限额范围内。 */
  public void requireUsable(String merchantId, String chainCode, String tokenSymbol, BigDecimal amount) {
    usableChannel(merchantId, chainCode, tokenSymbol, amount);
  }

  /** 返回指定商户、链、币的可用配置；可用于固化订单费率快照。 */
  public MerchantPaymentChannelConfig usableChannel(
      String merchantId, String chainCode, String tokenSymbol, BigDecimal amount) {
    if (!StringUtils.hasText(merchantId) || !StringUtils.hasText(chainCode) || !StringUtils.hasText(tokenSymbol)) {
      throw new IllegalArgumentException("merchantId, chainCode and tokenSymbol are required");
    }
    MerchantPaymentChannelConfig config = mapper.selectEnabledUsableChannel(
        merchantId.trim(), normalize(chainCode), normalize(tokenSymbol));
    if (config == null) {
      throw new IllegalArgumentException("Merchant payment channel, chain or token is not enabled");
    }
    if (amount != null && belowMinimum(config, amount)) {
      throw new IllegalArgumentException("Order amount is lower than merchant channel minimum amount");
    }
    if (amount != null && aboveMaximum(config, amount)) {
      throw new IllegalArgumentException("Order amount is higher than merchant channel maximum amount");
    }
    return config;
  }

  private boolean amountWithinLimit(MerchantPaymentChannelConfig config, BigDecimal amount) {
    return amount == null || (!belowMinimum(config, amount) && !aboveMaximum(config, amount));
  }

  private boolean belowMinimum(MerchantPaymentChannelConfig config, BigDecimal amount) {
    return config.getMinOrderAmount() != null && amount.compareTo(config.getMinOrderAmount()) < 0;
  }

  private boolean aboveMaximum(MerchantPaymentChannelConfig config, BigDecimal amount) {
    return config.getMaxOrderAmount() != null
        && config.getMaxOrderAmount().compareTo(BigDecimal.ZERO) > 0
        && amount.compareTo(config.getMaxOrderAmount()) > 0;
  }

  private String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }
}
