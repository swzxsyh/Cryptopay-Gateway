package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAccount;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 商户资金余额账户 Mapper。 */
@Mapper
public interface MerchantBalanceAccountMapper extends BaseMapper<MerchantBalanceAccount> {

  @Insert("""
      INSERT INTO merchant_balance_account
        (merchant_id, balance_token, available_balance, frozen_balance, total_income, total_withdrawn, status, created_at, updated_at)
      VALUES
        (#{merchantId}, #{token}, #{amount}, 0, #{amount}, 0, 'ENABLED', NOW(3), NOW(3))
      ON DUPLICATE KEY UPDATE
        available_balance = available_balance + VALUES(available_balance),
        total_income = total_income + VALUES(total_income),
        updated_at = NOW(3)
      """)
  int credit(@Param("merchantId") String merchantId, @Param("token") String token, @Param("amount") BigDecimal amount);

  @Update("""
      UPDATE merchant_balance_account
      SET available_balance = available_balance - #{amount},
          frozen_balance = frozen_balance + #{amount},
          updated_at = NOW(3)
      WHERE merchant_id = #{merchantId}
        AND balance_token = #{token}
        AND status = 'ENABLED'
        AND available_balance >= #{amount}
      """)
  int freezeForWithdraw(@Param("merchantId") String merchantId, @Param("token") String token, @Param("amount") BigDecimal amount);

  @Update("""
      UPDATE merchant_balance_account
      SET frozen_balance = frozen_balance - #{amount},
          total_withdrawn = total_withdrawn + #{amount},
          updated_at = NOW(3)
      WHERE merchant_id = #{merchantId}
        AND balance_token = #{token}
        AND frozen_balance >= #{amount}
      """)
  int completeWithdraw(@Param("merchantId") String merchantId, @Param("token") String token, @Param("amount") BigDecimal amount);

  @Update("""
      UPDATE merchant_balance_account
      SET available_balance = available_balance + #{amount},
          frozen_balance = frozen_balance - #{amount},
          updated_at = NOW(3)
      WHERE merchant_id = #{merchantId}
        AND balance_token = #{token}
        AND frozen_balance >= #{amount}
      """)
  int releaseWithdraw(@Param("merchantId") String merchantId, @Param("token") String token, @Param("amount") BigDecimal amount);

  @Update("""
      UPDATE merchant_balance_account
      SET available_balance = available_balance - #{amount},
          updated_at = NOW(3)
      WHERE merchant_id = #{merchantId}
        AND balance_token = #{token}
        AND status = 'ENABLED'
        AND available_balance >= #{amount}
      """)
  int debitAvailable(@Param("merchantId") String merchantId, @Param("token") String token, @Param("amount") BigDecimal amount);
}
