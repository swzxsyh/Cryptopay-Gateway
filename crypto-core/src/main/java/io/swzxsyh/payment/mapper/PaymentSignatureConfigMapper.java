package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentSignatureConfig;
import org.apache.ibatis.annotations.Mapper;

/** 签名配置访问层。 */
@Mapper
public interface PaymentSignatureConfigMapper extends BaseMapper<PaymentSignatureConfig> {}
