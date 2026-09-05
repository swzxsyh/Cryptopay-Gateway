package io.swzxsyh.manager.message;

/** 管理端消息推送抽象，后续接短信、邮件、企微等只需要新增实现。 */
public interface ManagerMessageSender {

  ManagerMessageSendResult send(ManagerMessageRequest request);
}
