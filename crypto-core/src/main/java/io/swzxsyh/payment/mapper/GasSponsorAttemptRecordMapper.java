package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.GasSponsorAttemptRecord;
import org.apache.ibatis.annotations.Mapper;

/** Gas 代付尝试记录 Mapper。 */
@Mapper
public interface GasSponsorAttemptRecordMapper extends BaseMapper<GasSponsorAttemptRecord> {
}
