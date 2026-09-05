package io.swzxsyh.manager.api.dto;

import java.time.LocalDateTime;

public record ManagerScannerCheckpointRequest(
    String chain,
    Long latestObservedBlock,
    Long lastConfirmedBlock,
    LocalDateTime createdAt) {}
