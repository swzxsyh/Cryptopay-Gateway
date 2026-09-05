package io.swzxsyh.manager.approval;

import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import java.lang.reflect.Method;

/** 审批拦截后构造原方法兼容返回值的工厂。 */
public interface ApprovalResponseFactory {

  /** 是否支持当前方法返回类型和业务类型。 */
  boolean supports(Method method, String bizType);

  /** 构造“已提交审批”的返回对象。 */
  Object build(ManagerApprovalRequest approval, Object payload);
}
