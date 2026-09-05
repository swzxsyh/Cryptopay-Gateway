package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalStepDefinition;
import org.apache.ibatis.annotations.Mapper;

/** 审批步骤定义 Mapper。 */
@Mapper
public interface ManagerApprovalStepDefinitionMapper extends BaseMapper<ManagerApprovalStepDefinition> {}
