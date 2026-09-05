package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantPaymentChannelConfig;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 商户支付通道费率配置 Mapper。 */
@Mapper
public interface MerchantPaymentChannelConfigMapper extends BaseMapper<MerchantPaymentChannelConfig> {

  /** 查询商户主体、商户通道、平台链和平台代币都处于启用状态的支付通道。 */
  @Select("""
      <script>
      SELECT m.*
      FROM merchant_payment_channel_config m
      INNER JOIN merchant_info mi
        ON mi.merchant_id = m.merchant_id
       AND mi.status = 'ENABLED'
      INNER JOIN payment_chain_config c
        ON c.chain_code = m.chain_code
       AND c.enabled = 1
      INNER JOIN payment_token_config t
        ON t.chain_code = m.chain_code
       AND t.token_symbol = m.token_symbol
       AND t.enabled = 1
      WHERE m.merchant_id = #{merchantId}
        AND m.enabled = 1
      <if test="tokenSymbol != null and tokenSymbol != ''">
        AND m.token_symbol = #{tokenSymbol}
      </if>
      ORDER BY m.chain_code ASC, m.token_symbol ASC
      </script>
      """)
  List<MerchantPaymentChannelConfig> selectEnabledUsableChannels(
      @Param("merchantId") String merchantId,
      @Param("tokenSymbol") String tokenSymbol);

  /** 查询指定商户、链、币是否完整可用；商户主体、商户通道、链和代币任一关闭都会查不到。 */
  @Select("""
      SELECT m.*
      FROM merchant_payment_channel_config m
      INNER JOIN merchant_info mi
        ON mi.merchant_id = m.merchant_id
       AND mi.status = 'ENABLED'
      INNER JOIN payment_chain_config c
        ON c.chain_code = m.chain_code
       AND c.enabled = 1
      INNER JOIN payment_token_config t
        ON t.chain_code = m.chain_code
       AND t.token_symbol = m.token_symbol
       AND t.enabled = 1
      WHERE m.merchant_id = #{merchantId}
        AND m.chain_code = #{chainCode}
        AND m.token_symbol = #{tokenSymbol}
        AND m.enabled = 1
      LIMIT 1
      """)
  MerchantPaymentChannelConfig selectEnabledUsableChannel(
      @Param("merchantId") String merchantId,
      @Param("chainCode") String chainCode,
      @Param("tokenSymbol") String tokenSymbol);
}
