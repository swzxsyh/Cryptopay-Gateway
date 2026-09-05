package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantWithdrawRecord;
import org.apache.ibatis.annotations.Mapper;

/** 商户提现流水 Mapper。 */
@Mapper
public interface MerchantWithdrawRecordMapper extends BaseMapper<MerchantWithdrawRecord> {
}
