package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentChainScannerConfig;
import org.apache.ibatis.annotations.Mapper;

/** 单链扫描策略覆盖配置 Mapper。 */
@Mapper
public interface PaymentChainScannerConfigMapper extends BaseMapper<PaymentChainScannerConfig> {}
