package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PlatformCipherKey;
import org.apache.ibatis.annotations.Mapper;

/** 平台加密密钥 Mapper。 */
@Mapper
public interface PlatformCipherKeyMapper extends BaseMapper<PlatformCipherKey> {
}
