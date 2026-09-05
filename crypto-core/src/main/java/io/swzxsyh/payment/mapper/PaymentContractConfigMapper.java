package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentContractConfig;
import org.apache.ibatis.annotations.Mapper;

/** 合约配置访问层。 */
@Mapper
public interface PaymentContractConfigMapper extends BaseMapper<PaymentContractConfig> {}
