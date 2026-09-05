package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentChainConfig;
import org.apache.ibatis.annotations.Mapper;

/** 链配置访问层。 */
@Mapper
public interface PaymentChainConfigMapper extends BaseMapper<PaymentChainConfig> {}
