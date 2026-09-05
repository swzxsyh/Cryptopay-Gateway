package io.swzxsyh.manager.approval;

import io.swzxsyh.manager.api.dto.ManagerMerchantDtos.BalanceAdjustRequest;
import io.swzxsyh.manager.application.ManagerApprovalApplicationService;
import io.swzxsyh.payment.persistence.entity.ManagerApprovalRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** manager 审批切面：有启用审批配置则拦截成审批单，没有配置则直接执行原业务。 */
@Aspect
@Component
public class ManagerApprovalAspect {

  private final ManagerApprovalApplicationService approvalService;
  private final ObjectMapper objectMapper;
  private final List<ApprovalResponseFactory> responseFactories;

  public ManagerApprovalAspect(
      ManagerApprovalApplicationService approvalService,
      ObjectMapper objectMapper,
      List<ApprovalResponseFactory> responseFactories) {
    this.approvalService = approvalService;
    this.objectMapper = objectMapper;
    this.responseFactories = responseFactories;
  }

  @Around("@annotation(required)")
  public Object requireApproval(ProceedingJoinPoint joinPoint, ManagerApprovalRequired required) throws Throwable {
    if (!approvalService.hasEnabledFlow(required.bizType())) {
      return joinPoint.proceed();
    }
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    Object payload = payload(joinPoint.getArgs(), required.payloadArgIndex());
    ManagerApprovalRequest approval = approvalService.submit(
        required.bizType(),
        bizKey(required.bizType(), payload),
        title(required, payload),
        objectMapper.writeValueAsString(payload),
        null,
        remark(payload));
    return responseFactories.stream()
        .filter(factory -> factory.supports(method, required.bizType()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("approval response factory not found"))
        .build(approval, payload);
  }

  private Object payload(Object[] args, int index) {
    if (args == null || args.length == 0 || index < 0 || index >= args.length || args[index] == null) {
      throw new IllegalArgumentException("approval payload is required");
    }
    return args[index];
  }

  private String title(ManagerApprovalRequired required, Object payload) {
    if (StringUtils.hasText(required.title())) {
      return required.title();
    }
    if (payload instanceof BalanceAdjustRequest request) {
      return "商户余额调账 " + request.merchantId() + " " + request.token()
          + " " + request.direction() + " " + request.amount();
    }
    return required.bizType() + " 审批";
  }

  private String bizKey(String bizType, Object payload) {
    if (payload instanceof BalanceAdjustRequest request) {
      return request.merchantId() + ":" + request.token() + ":" + request.direction() + ":" + System.nanoTime();
    }
    return bizType + ":" + System.nanoTime();
  }

  private String remark(Object payload) {
    if (payload instanceof BalanceAdjustRequest request) {
      return request.operatorNote();
    }
    return null;
  }
}
