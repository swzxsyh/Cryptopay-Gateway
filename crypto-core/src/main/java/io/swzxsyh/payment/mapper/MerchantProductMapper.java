package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantProduct;
import org.apache.ibatis.annotations.Mapper;

/** 商户产品库 Mapper。 */
@Mapper
public interface MerchantProductMapper extends BaseMapper<MerchantProduct> {
}
