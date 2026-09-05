package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentExceptionOrder;
import org.apache.ibatis.annotations.Mapper;

/** 支付异常单 Mapper。 */
@Mapper
public interface PaymentExceptionOrderMapper extends BaseMapper<PaymentExceptionOrder> {
}
