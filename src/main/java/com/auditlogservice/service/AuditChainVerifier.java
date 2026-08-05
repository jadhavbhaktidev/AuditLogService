package com.auditlogservice.service;

import java.util.List;

import com.auditlogservice.domain.AuditRecord;
import com.auditlogservice.dto.AuditVerificationIssue;
import org.springframework.stereotype.Component;

@Component
public class AuditChainVerifier {

    private final AuditChainHasher auditChainHasher;

    public AuditChainVerifier(AuditChainHasher auditChainHasher) {
        this.auditChainHasher = auditChainHasher;
    }

    public AuditVerificationIssue verify(List<AuditRecord> records) {
        String expectedPrevHash = AuditChainHasher.GENESIS_PREV_HASH;
        Long expectedSequence = null;

        for (AuditRecord record : records) {
            if (expectedSequence != null && !record.getSequenceNumber().equals(expectedSequence)) {
                return new AuditVerificationIssue(false, record.getSequenceNumber(),
                        "SEQUENCE_GAP_OR_REORDER", "Sequence gap or reorder detected", expectedSequence - 1);
            }

            if (!expectedPrevHash.equals(record.getPrevHash())) {
                return new AuditVerificationIssue(false, record.getSequenceNumber(),
                        "PREV_HASH_MISMATCH", "Previous hash does not match the chain", record.getSequenceNumber() - 1);
            }

            String recalculatedHash = auditChainHasher.sha256Hex(auditChainHasher.canonicalPayload(record));
            if (!recalculatedHash.equals(record.getRecordHash())) {
                return new AuditVerificationIssue(false, record.getSequenceNumber(),
                        "RECORD_HASH_MISMATCH", "Record hash does not match stored content", record.getSequenceNumber());
            }

            expectedPrevHash = record.getRecordHash();
            expectedSequence = record.getSequenceNumber() + 1;
        }

        return new AuditVerificationIssue(true, null, null, "Chain intact", records.size());
    }
}
