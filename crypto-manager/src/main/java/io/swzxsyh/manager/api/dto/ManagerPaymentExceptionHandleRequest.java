package io.swzxsyh.manager.api.dto;

/** 管理端异常单处理请求。 */
public record ManagerPaymentExceptionHandleRequest(
    String status,
    String operator,
    String operatorNote) {}
