package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentKytConfig;
import org.apache.ibatis.annotations.Mapper;

/** KYT 配置访问层。 */
@Mapper
public interface PaymentKytConfigMapper extends BaseMapper<PaymentKytConfig> {}
