package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PaymentAuditRecord;
import org.apache.ibatis.annotations.Mapper;

/** 审计记录 Mapper。 */
@Mapper
public interface PaymentAuditRecordMapper extends BaseMapper<PaymentAuditRecord> {
}
