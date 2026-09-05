package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.domain.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

/** 普通支付订单 Mapper。 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
