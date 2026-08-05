package com.auditlogservice.dto;

import java.time.OffsetDateTime;

public record RetentionRunResponse(
        int archivedCount,
        int retentionDays,
        long cutoffEpochMillis,
        OffsetDateTime executedAt) {
}
