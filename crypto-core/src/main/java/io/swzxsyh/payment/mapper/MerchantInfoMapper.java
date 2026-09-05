package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantInfo;
import org.apache.ibatis.annotations.Mapper;

/** 商户主数据 Mapper。 */
@Mapper
public interface MerchantInfoMapper extends BaseMapper<MerchantInfo> {
}
