package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentGasConfig;
import org.apache.ibatis.annotations.Mapper;

/** Gas 策略配置访问层。 */
@Mapper
public interface PaymentGasConfigMapper extends BaseMapper<PaymentGasConfig> {}
