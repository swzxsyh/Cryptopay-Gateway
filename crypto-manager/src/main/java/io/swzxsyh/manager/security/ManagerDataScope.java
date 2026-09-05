package io.swzxsyh.manager.security;

/** 当前管理员拥有的数据权限范围。 */
public record ManagerDataScope(String scopeType, String scopeValue) {}
