package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentSubscriptionConfig;
import org.apache.ibatis.annotations.Mapper;

/** 订阅配置访问层。 */
@Mapper
public interface PaymentSubscriptionConfigMapper extends BaseMapper<PaymentSubscriptionConfig> {}
