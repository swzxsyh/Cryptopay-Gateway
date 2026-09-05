package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.CashierTokenMapping;
import org.apache.ibatis.annotations.Mapper;

/** 收银台短码映射访问层。 */
@Mapper
public interface CashierTokenMappingMapper extends BaseMapper<CashierTokenMapping> {}
