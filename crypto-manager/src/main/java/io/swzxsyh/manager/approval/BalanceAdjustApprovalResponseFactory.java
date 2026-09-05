package io.swzxsyh.manager.approval;

import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustRequest;
import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustResponse;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import java.lang.reflect.Method;
import org.springframework.stereotype.Component;

/** 调账审批被拦截时，返回给前端的兼容响应。 */
@Component
public class BalanceAdjustApprovalResponseFactory implements ApprovalResponseFactory {

  @Override
  public boolean supports(Method method, String bizType) {
    return "MERCHANT_BALANCE_ADJUST".equalsIgnoreCase(bizType)
        && BalanceAdjustResponse.class.isAssignableFrom(method.getReturnType());
  }

  @Override
  public Object build(ManagerApprovalRequest approval, Object payload) {
    BalanceAdjustRequest request = (BalanceAdjustRequest) payload;
    return new BalanceAdjustResponse(
        approval.getApprovalNo(),
        null,
        request.merchantId(),
        request.token(),
        request.direction(),
        request.amount(),
        approval.getStatus());
  }
}
