package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentKytAddressRule;
import org.apache.ibatis.annotations.Mapper;

/** KYT 地址规则访问层。 */
@Mapper
public interface PaymentKytAddressRuleMapper extends BaseMapper<PaymentKytAddressRule> {}
