package io.swzxsyh.manager.approval;

import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;

/** 审批通过后的业务执行器；不同 bizType 通过不同实现承接。 */
public interface ApprovalBusinessHandler {

  /** 当前处理器支持的业务类型。 */
  String bizType();

  /** 审批满足人数后执行真实业务动作，必须保证幂等。 */
  void executeApproved(ManagerApprovalRequest request);
}
