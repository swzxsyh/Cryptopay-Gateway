package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentGatewayConfig;
import org.apache.ibatis.annotations.Mapper;

/** x402 网关配置访问层。 */
@Mapper
public interface PaymentGatewayConfigMapper extends BaseMapper<PaymentGatewayConfig> {}
