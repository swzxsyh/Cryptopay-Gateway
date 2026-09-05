package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentDerivedAddressConfig;
import org.apache.ibatis.annotations.Mapper;

/** 派生地址配置访问层。 */
@Mapper
public interface PaymentDerivedAddressConfigMapper extends BaseMapper<PaymentDerivedAddressConfig> {}
