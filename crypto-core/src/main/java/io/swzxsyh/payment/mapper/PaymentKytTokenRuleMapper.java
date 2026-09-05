package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentKytTokenRule;
import org.apache.ibatis.annotations.Mapper;

/** KYT Token 规则访问层。 */
@Mapper
public interface PaymentKytTokenRuleMapper extends BaseMapper<PaymentKytTokenRule> {}
