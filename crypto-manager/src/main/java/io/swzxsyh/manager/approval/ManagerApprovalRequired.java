package io.swzxsyh.manager.approval;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标记高风险 manager 操作需要按配置判断是否进入多人审批。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManagerApprovalRequired {

  /** 审批业务类型，用于匹配 manager_approval_definition.biz_type 和业务 handler。 */
  String bizType();

  /** 默认审批标题；为空时由切面根据业务参数生成。 */
  String title() default "";

  /** 作为审批业务快照的参数下标，默认取第一个参数。 */
  int payloadArgIndex() default 0;
}
