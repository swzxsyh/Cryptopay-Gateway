package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentTokenConfig;
import org.apache.ibatis.annotations.Mapper;

/** Token 配置访问层。 */
@Mapper
public interface PaymentTokenConfigMapper extends BaseMapper<PaymentTokenConfig> {}
