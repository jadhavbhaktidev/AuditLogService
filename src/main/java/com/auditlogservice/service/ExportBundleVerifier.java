package com.auditlogservice.service;

import java.util.List;

import com.auditlogservice.dto.ExportBundleResponse;
import com.auditlogservice.dto.ExportBundleVerificationResponse;
import com.auditlogservice.dto.ExportedAuditRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class ExportBundleVerifier {

    private final ObjectMapper objectMapper;
    private final AuditChainHasher auditChainHasher;

    public ExportBundleVerifier(ObjectMapper objectMapper, AuditChainHasher auditChainHasher) {
        this.objectMapper = objectMapper;
        this.auditChainHasher = auditChainHasher;
    }

    public String computeChecksum(ExportBundleResponse bundle) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("exportId", bundle.exportId());
        root.put("exportedAt", bundle.exportedAt() == null ? null : bundle.exportedAt().toString());
        root.put("actorId", bundle.actorId());
        root.put("resourceType", bundle.resourceType());
        root.put("resourceId", bundle.resourceId());
        root.put("includeArchived", bundle.includeArchived());
        root.put("recordCount", bundle.recordCount());
        if (bundle.firstSequenceNumber() == null) {
            root.putNull("firstSequenceNumber");
        } else {
            root.put("firstSequenceNumber", bundle.firstSequenceNumber());
        }
        if (bundle.lastSequenceNumber() == null) {
            root.putNull("lastSequenceNumber");
        } else {
            root.put("lastSequenceNumber", bundle.lastSequenceNumber());
        }
        root.put("chainHeadHash", bundle.chainHeadHash());
        root.put("sourceChainIntact", bundle.sourceChainIntact());
        root.put("sourceCheckedRecords", bundle.sourceCheckedRecords());

        ArrayNode recordsNode = root.putArray("records");
        for (ExportedAuditRecord record : bundle.records()) {
            ObjectNode item = recordsNode.addObject();
            item.put("sequenceNumber", record.sequenceNumber());
            item.put("eventType", record.eventType());
            item.put("actorId", record.actorId());
            item.put("resourceType", record.resourceType());
            item.put("resourceId", record.resourceId());
            item.set("payload", record.payload());
            item.put("timestamp", record.timestamp() == null ? null : record.timestamp().toString());
            item.put("prevHash", record.prevHash());
            item.put("recordHash", record.recordHash());
            item.put("archivedAt", record.archivedAt() == null ? null : record.archivedAt().toString());
            item.put("redactionState", record.redactionState());
            ArrayNode redactedFieldsNode = item.putArray("redactedFields");
            record.redactedFields().forEach(redactedFieldsNode::add);
            item.put("redactionProofArtifact", record.redactionProofArtifact());
        }

        try {
            return auditChainHasher.sha256Hex(objectMapper.writeValueAsString(root));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to compute export bundle checksum", exception);
        }
    }

    public ExportBundleVerificationResponse verify(ExportBundleResponse bundle) {
        if (bundle == null) {
            return new ExportBundleVerificationResponse(false,
                    "Export bundle is required", "BUNDLE_MISSING", 0, null, null);
        }

        if (bundle.records() == null) {
            return new ExportBundleVerificationResponse(false,
                    "Export bundle records are required", "RECORDS_MISSING", 0, null, null);
        }

        String expectedChecksum = computeChecksum(bundle);
        if (!expectedChecksum.equals(bundle.bundleChecksum())) {
            return new ExportBundleVerificationResponse(false,
                    "Bundle checksum mismatch", "CHECKSUM_MISMATCH", bundle.records().size(),
                    expectedChecksum, bundle.bundleChecksum());
        }

        if (bundle.recordCount() != bundle.records().size()) {
            return new ExportBundleVerificationResponse(false,
                    "Record count does not match record payload", "RECORD_COUNT_MISMATCH",
                    bundle.records().size(), String.valueOf(bundle.records().size()), String.valueOf(bundle.recordCount()));
        }

        List<ExportedAuditRecord> records = bundle.records();
        Long previousSequence = null;
        String previousRecordHash = null;
        for (ExportedAuditRecord record : records) {
            if (record.sequenceNumber() == null) {
                return new ExportBundleVerificationResponse(false,
                        "Record sequence number is required", "SEQUENCE_MISSING", 0, null, null);
            }
            if (previousSequence != null && record.sequenceNumber() <= previousSequence) {
                return new ExportBundleVerificationResponse(false,
                        "Sequence ordering is invalid", "SEQUENCE_ORDER_INVALID", record.sequenceNumber(),
                        String.valueOf(previousSequence + 1), String.valueOf(record.sequenceNumber()));
            }

            if (previousSequence != null
                    && record.sequenceNumber().equals(previousSequence + 1)
                    && previousRecordHash != null
                    && !previousRecordHash.equals(record.prevHash())) {
                return new ExportBundleVerificationResponse(false,
                        "Adjacent chain linkage mismatch", "ADJACENT_PREV_HASH_MISMATCH", record.sequenceNumber(),
                        previousRecordHash, record.prevHash());
            }

            if (!isSha256Hex(record.recordHash())) {
                return new ExportBundleVerificationResponse(false,
                        "Record hash is not a valid SHA-256 hex value", "RECORD_HASH_FORMAT_INVALID", record.sequenceNumber(),
                        "64-char lowercase hex", record.recordHash());
            }

            previousSequence = record.sequenceNumber();
            previousRecordHash = record.recordHash();
        }

        return new ExportBundleVerificationResponse(true,
                "Bundle checksum and structure verified", null, records.size(), null, null);
    }

    private boolean isSha256Hex(String value) {
        return value != null && value.matches("^[a-f0-9]{64}$");
    }
}
