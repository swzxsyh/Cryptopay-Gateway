package io.swzxsyh.manager.api.dto;

public record ManagerCsrfResponse(String headerName, String parameterName, String token) {}
