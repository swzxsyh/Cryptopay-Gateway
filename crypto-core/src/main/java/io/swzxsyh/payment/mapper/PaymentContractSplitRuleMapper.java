package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentContractSplitRule;
import org.apache.ibatis.annotations.Mapper;

/** 合约分账规则访问层。 */
@Mapper
public interface PaymentContractSplitRuleMapper extends BaseMapper<PaymentContractSplitRule> {}
