package com.auditlogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import com.auditlogservice.domain.AuditRecord;
import org.junit.jupiter.api.Test;

class AuditChainVerifierTest {

    private final AuditChainHasher hasher = new AuditChainHasher(new com.fasterxml.jackson.databind.ObjectMapper());
    private final AuditChainVerifier verifier = new AuditChainVerifier(hasher);

    @Test
    void verifyReturnsIntactForMatchingChain() {
        AuditRecord first = buildRecord(1L, AuditChainHasher.GENESIS_PREV_HASH, "first-payload");
        AuditRecord second = buildRecord(2L, first.getRecordHash(), "second-payload");

        var result = verifier.verify(List.of(first, second));

        assertThat(result.intact()).isTrue();
        assertThat(result.checkedRecords()).isEqualTo(2);
    }

    @Test
    void verifyDetectsPrevHashMismatch() {
        AuditRecord first = buildRecord(1L, AuditChainHasher.GENESIS_PREV_HASH, "first-payload");
        AuditRecord second = buildRecord(2L, "wrong-prev", "second-payload");

        var result = verifier.verify(List.of(first, second));

        assertThat(result.intact()).isFalse();
        assertThat(result.violationType()).isEqualTo("PREV_HASH_MISMATCH");
    }

    private AuditRecord buildRecord(long sequenceNumber, String prevHash, String payload) {
        AuditRecord record = new AuditRecord();
        record.setSequenceNumber(sequenceNumber);
        record.setEventType("EVENT");
        record.setActorId("actor");
        record.setResourceType("RESOURCE");
        record.setResourceId("resource");
        record.setPayloadJson("{\"value\":\"" + payload + "\"}");
        record.setEventTimestamp(OffsetDateTime.parse("2026-08-05T10:15:30Z").toInstant().toEpochMilli());
        record.setPrevHash(prevHash);
        record.setRecordHash(hasher.sha256Hex(hasher.canonicalPayload(record)));
        return record;
    }
}
