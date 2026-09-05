package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.RequestSignatureReplayRecord;
import org.apache.ibatis.annotations.Mapper;

/** 请求签名防重放记录 Mapper。 */
@Mapper
public interface RequestSignatureReplayRecordMapper extends BaseMapper<RequestSignatureReplayRecord> {
}
