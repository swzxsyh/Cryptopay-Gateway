package io.swzxsyh.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import org.apache.ibatis.annotations.Mapper;

/** 通用审批申请 Mapper。 */
@Mapper
public interface ManagerApprovalRequestMapper extends BaseMapper<ManagerApprovalRequest> {}
