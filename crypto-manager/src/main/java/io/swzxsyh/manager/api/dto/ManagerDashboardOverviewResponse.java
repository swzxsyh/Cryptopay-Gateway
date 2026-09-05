package io.swzxsyh.manager.api.dto;

import java.util.Map;

public record ManagerDashboardOverviewResponse(
    Map<String, Long> orderStatusCounts,
    Map<String, Long> callbackStatusCounts,
    Map<String, Long> paymentExceptionStatusCounts,
    Map<String, Long> rawChainLogCounts,
    Map<String, Long> settlementStatusCounts,
    Map<String, Long> addressPoolStatusCounts,
    Map<String, Long> subscriptionStatusCounts,
    Map<String, Long> subscriptionBillingStatusCounts,
    long scannerCount,
    long auditEventCount) {}
