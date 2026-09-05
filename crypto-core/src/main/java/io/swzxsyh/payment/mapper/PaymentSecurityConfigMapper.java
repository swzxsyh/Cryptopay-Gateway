package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentSecurityConfig;
import org.apache.ibatis.annotations.Mapper;

/** 安全配置访问层。 */
@Mapper
public interface PaymentSecurityConfigMapper extends BaseMapper<PaymentSecurityConfig> {}
