package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.MerchantSignatureKey;
import org.apache.ibatis.annotations.Mapper;

/** 商户验签公钥 Mapper。 */
@Mapper
public interface MerchantSignatureKeyMapper extends BaseMapper<MerchantSignatureKey> {
}
