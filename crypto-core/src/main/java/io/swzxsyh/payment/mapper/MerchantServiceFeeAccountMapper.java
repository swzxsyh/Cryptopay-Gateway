package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantServiceFeeAccount;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 商户服务费台账 Mapper。 */
@Mapper
public interface MerchantServiceFeeAccountMapper extends BaseMapper<MerchantServiceFeeAccount> {

  @Insert("""
      INSERT INTO merchant_service_fee_account (merchant_id, fee_token, balance, created_at, updated_at)
      VALUES (#{merchantId}, #{feeToken}, #{amount}, NOW(3), NOW(3))
      ON DUPLICATE KEY UPDATE
        balance = balance + VALUES(balance),
        updated_at = NOW(3)
      """)
  int credit(@Param("merchantId") String merchantId,
      @Param("feeToken") String feeToken,
      @Param("amount") BigDecimal amount);

  @Update("""
      UPDATE merchant_service_fee_account
      SET balance = balance - #{amount},
          updated_at = NOW(3)
      WHERE merchant_id = #{merchantId}
        AND fee_token = #{feeToken}
        AND balance >= #{amount}
      """)
  int deduct(@Param("merchantId") String merchantId,
      @Param("feeToken") String feeToken,
      @Param("amount") BigDecimal amount);

  @Select("""
      SELECT balance
      FROM merchant_service_fee_account
      WHERE merchant_id = #{merchantId}
        AND fee_token = #{feeToken}
      """)
  BigDecimal selectBalance(@Param("merchantId") String merchantId,
      @Param("feeToken") String feeToken);
}
