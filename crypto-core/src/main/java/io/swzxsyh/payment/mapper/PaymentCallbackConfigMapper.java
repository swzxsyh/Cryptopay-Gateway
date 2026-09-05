package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackConfig;
import org.apache.ibatis.annotations.Mapper;

/** 回调配置访问层。 */
@Mapper
public interface PaymentCallbackConfigMapper extends BaseMapper<PaymentCallbackConfig> {}
