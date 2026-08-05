package com.auditlogservice.dto;

import java.util.List;

public record AuditEventQueryResponse(
        List<AuditEventResponse> items,
        long page,
        long size,
        long totalElements,
        long totalPages) {
}
