package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantBalanceFlowRecord;
import org.apache.ibatis.annotations.Mapper;

/** 商户资金流水 Mapper。 */
@Mapper
public interface MerchantBalanceFlowRecordMapper extends BaseMapper<MerchantBalanceFlowRecord> {
}
