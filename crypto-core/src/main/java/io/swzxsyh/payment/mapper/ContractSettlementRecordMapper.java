package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ContractSettlementRecord;
import org.apache.ibatis.annotations.Mapper;

/** 智能合约分账总记录 Mapper。 */
@Mapper
public interface ContractSettlementRecordMapper extends BaseMapper<ContractSettlementRecord> {
}
