package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentScannerConfig;
import org.apache.ibatis.annotations.Mapper;

/** 链扫描配置访问层。 */
@Mapper
public interface PaymentScannerConfigMapper extends BaseMapper<PaymentScannerConfig> {}
