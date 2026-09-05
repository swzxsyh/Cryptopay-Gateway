package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalAction;
import org.apache.ibatis.annotations.Mapper;

/** 通用审批动作 Mapper。 */
@Mapper
public interface ManagerApprovalActionMapper extends BaseMapper<ManagerApprovalAction> {}
