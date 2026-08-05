package com.auditlogservice.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record AuditEventResponse(
        Long id,
        Long sequenceNumber,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode payload,
        OffsetDateTime timestamp,
        String prevHash,
        String recordHash) {
}
