package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.subscription.SubscriptionOrder;
import org.apache.ibatis.annotations.Mapper;

/** 订阅订单 Mapper。 */
@Mapper
public interface SubscriptionOrderMapper extends BaseMapper<SubscriptionOrder> {
}
