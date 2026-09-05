package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

/** 幂等记录 Mapper。 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecord> {
}
