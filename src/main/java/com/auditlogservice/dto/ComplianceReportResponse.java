package com.auditlogservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ComplianceReportResponse(
        OffsetDateTime generatedAt,
        String resourceType,
        String actorId,
        String resourceId,
        OffsetDateTime from,
        OffsetDateTime to,
        boolean includeArchived,
        long page,
        long size,
        long totalElements,
        long totalPages,
        Long firstSequenceNumber,
        Long lastSequenceNumber,
        boolean sourceChainIntact,
        long sourceCheckedRecords,
        List<ComplianceReportItem> items) {
}
