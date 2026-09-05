package io.swzxsyh.manager.message;

import java.util.Map;

/** 消息发送请求，屏蔽短信、邮件等 provider 的差异。 */
public record ManagerMessageRequest(
    String templateCode,
    String channel,
    String receiver,
    Map<String, ?> variables) {}
