package com.auditlogservice.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record ComplianceReportItem(
        Long sequenceNumber,
        String eventType,
        String actorId,
        String resourceId,
        JsonNode payload,
        OffsetDateTime timestamp,
        String recordHash) {
}
