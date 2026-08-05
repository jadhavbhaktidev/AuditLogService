package com.auditlogservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RedactionResponse(
        Long sequenceNumber,
        List<String> redactedFields,
        String reason,
        String approvedBy,
        OffsetDateTime approvedAt,
        String proofArtifact,
        String redactionState) {
}
