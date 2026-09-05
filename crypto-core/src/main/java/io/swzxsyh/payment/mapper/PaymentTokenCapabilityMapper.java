package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentTokenCapability;
import org.apache.ibatis.annotations.Mapper;

/** Token 能力配置访问层。 */
@Mapper
public interface PaymentTokenCapabilityMapper extends BaseMapper<PaymentTokenCapability> {}
