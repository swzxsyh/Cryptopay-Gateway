package io.swzxsyh.manager.message;

/** 消息推送结果，记录 provider 是否接收成功。 */
public record ManagerMessageSendResult(
    boolean success,
    String provider,
    String providerMessageId,
    String message) {}
