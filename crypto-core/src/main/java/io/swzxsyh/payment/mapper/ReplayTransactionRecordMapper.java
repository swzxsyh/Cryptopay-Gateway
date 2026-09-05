package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ReplayTransactionRecord;
import org.apache.ibatis.annotations.Mapper;

/** 链上交易重放记录 Mapper。 */
@Mapper
public interface ReplayTransactionRecordMapper extends BaseMapper<ReplayTransactionRecord> {
}
