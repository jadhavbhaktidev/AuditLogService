package com.auditlogservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record ExportedAuditRecord(
        Long sequenceNumber,
        String eventType,
        String actorId,
        String resourceType,
        String resourceId,
        JsonNode payload,
        OffsetDateTime timestamp,
        String prevHash,
        String recordHash,
        OffsetDateTime archivedAt,
        String redactionState,
        List<String> redactedFields,
        String redactionProofArtifact) {
}
