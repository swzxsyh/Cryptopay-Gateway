package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentReconciliationRecord;
import org.apache.ibatis.annotations.Mapper;

/** 支付对账结果 Mapper。 */
@Mapper
public interface PaymentReconciliationRecordMapper extends BaseMapper<PaymentReconciliationRecord> {}
