package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentCashierConfig;
import org.apache.ibatis.annotations.Mapper;

/** 收银台配置访问层。 */
@Mapper
public interface PaymentCashierConfigMapper extends BaseMapper<PaymentCashierConfig> {}
