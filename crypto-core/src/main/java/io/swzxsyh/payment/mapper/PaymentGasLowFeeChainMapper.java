package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentGasLowFeeChain;
import org.apache.ibatis.annotations.Mapper;

/** 低费链配置访问层。 */
@Mapper
public interface PaymentGasLowFeeChainMapper extends BaseMapper<PaymentGasLowFeeChain> {}
