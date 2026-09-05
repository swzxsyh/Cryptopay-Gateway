package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentCallbackDeliveryRecord;
import org.apache.ibatis.annotations.Mapper;

/** 回调投递记录 Mapper。 */
@Mapper
public interface PaymentCallbackDeliveryRecordMapper extends BaseMapper<PaymentCallbackDeliveryRecord> {
}
