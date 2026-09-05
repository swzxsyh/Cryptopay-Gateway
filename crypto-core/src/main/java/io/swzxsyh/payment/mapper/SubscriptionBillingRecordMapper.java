package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.subscription.SubscriptionBillingRecord;
import org.apache.ibatis.annotations.Mapper;

/** 订阅周期账单 Mapper。 */
@Mapper
public interface SubscriptionBillingRecordMapper extends BaseMapper<SubscriptionBillingRecord> {
}
