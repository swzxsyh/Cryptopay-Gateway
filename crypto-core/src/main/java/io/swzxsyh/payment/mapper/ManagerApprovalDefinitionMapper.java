package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalDefinition;
import org.apache.ibatis.annotations.Mapper;

/** 审批流定义 Mapper。 */
@Mapper
public interface ManagerApprovalDefinitionMapper extends BaseMapper<ManagerApprovalDefinition> {}
