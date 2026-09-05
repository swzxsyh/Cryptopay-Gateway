package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.RawChainLog;
import org.apache.ibatis.annotations.Mapper;

/** 原始链上流水 Mapper。 */
@Mapper
public interface RawChainLogMapper extends BaseMapper<RawChainLog> {
}
