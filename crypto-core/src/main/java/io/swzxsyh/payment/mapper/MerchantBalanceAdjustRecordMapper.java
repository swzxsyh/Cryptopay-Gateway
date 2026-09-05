package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceAdjustRecord;
import org.apache.ibatis.annotations.Mapper;

/** 商户余额调账记录 Mapper。 */
@Mapper
public interface MerchantBalanceAdjustRecordMapper extends BaseMapper<MerchantBalanceAdjustRecord> {
}
