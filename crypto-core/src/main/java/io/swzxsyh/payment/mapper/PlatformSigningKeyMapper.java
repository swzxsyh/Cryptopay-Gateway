package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PlatformSigningKey;
import org.apache.ibatis.annotations.Mapper;

/** 平台签名密钥 Mapper。 */
@Mapper
public interface PlatformSigningKeyMapper extends BaseMapper<PlatformSigningKey> {
}
