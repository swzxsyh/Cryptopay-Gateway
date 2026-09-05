package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.PendingChainTransaction;
import org.apache.ibatis.annotations.Mapper;

/** 未确认链上交易 Mapper。 */
@Mapper
public interface PendingChainTransactionMapper extends BaseMapper<PendingChainTransaction> {
}
