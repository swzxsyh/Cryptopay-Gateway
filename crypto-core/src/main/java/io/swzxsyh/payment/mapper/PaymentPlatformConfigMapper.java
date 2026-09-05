package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentPlatformConfig;
import org.apache.ibatis.annotations.Mapper;

/** 平台基础配置访问层。 */
@Mapper
public interface PaymentPlatformConfigMapper extends BaseMapper<PaymentPlatformConfig> {}
