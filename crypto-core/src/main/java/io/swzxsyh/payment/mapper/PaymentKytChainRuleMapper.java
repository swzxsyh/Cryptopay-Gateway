package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentKytChainRule;
import org.apache.ibatis.annotations.Mapper;

/** KYT 链规则访问层。 */
@Mapper
public interface PaymentKytChainRuleMapper extends BaseMapper<PaymentKytChainRule> {}
