package io.swzxsyh.manager.api.dto;

import java.util.List;

public record ManagerPageResponse<T>(long page, long size, long total, List<T> records) {}
