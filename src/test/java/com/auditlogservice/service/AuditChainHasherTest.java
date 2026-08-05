package com.auditlogservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.auditlogservice.dto.AuditEventRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AuditChainHasherTest {

    private final AuditChainHasher hasher = new AuditChainHasher(new ObjectMapper());

    @Test
    void canonicalPayloadIsStableForEquivalentRequests() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode payload = objectMapper.readTree("{\"z\":2,\"a\":1}");
        AuditEventRequest request = new AuditEventRequest("USER_LOGIN", "actor-1", "ACCOUNT", "acct-1", payload, null);
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-05T10:15:30Z");

        String first = hasher.canonicalPayload(request, timestamp, AuditChainHasher.GENESIS_PREV_HASH);
        String second = hasher.canonicalPayload(request, timestamp, AuditChainHasher.GENESIS_PREV_HASH);

        assertThat(first).isEqualTo(second);
        assertThat(hasher.sha256Hex(first)).hasSize(64);
    }
}
