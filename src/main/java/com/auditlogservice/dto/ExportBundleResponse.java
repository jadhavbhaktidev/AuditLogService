package com.auditlogservice.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ExportBundleResponse(
        String exportId,
        OffsetDateTime exportedAt,
        String actorId,
        String resourceType,
        String resourceId,
        boolean includeArchived,
        long recordCount,
        Long firstSequenceNumber,
        Long lastSequenceNumber,
        String chainHeadHash,
        boolean sourceChainIntact,
        long sourceCheckedRecords,
        List<ExportedAuditRecord> records,
        String bundleChecksum) {
}
