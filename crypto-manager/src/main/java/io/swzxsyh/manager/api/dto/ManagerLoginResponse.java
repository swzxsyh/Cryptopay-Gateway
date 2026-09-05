package io.swzxsyh.manager.api.dto;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public record ManagerLoginResponse(
    String username,
    Collection<String> authorities,
    Collection<String> roles,
    Collection<String> functionPermissions,
    List<?> dataScopes,
    String tokenType,
    String accessToken,
    Instant expiresAt) {}
