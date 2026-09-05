package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ChainScannerCheckpoint;
import org.apache.ibatis.annotations.Mapper;

/** 链扫描进度表 Mapper。 */
@Mapper
public interface ChainScannerCheckpointMapper extends BaseMapper<ChainScannerCheckpoint> {
}
