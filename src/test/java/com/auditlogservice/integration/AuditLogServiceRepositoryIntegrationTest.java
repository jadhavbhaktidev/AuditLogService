package com.auditlogservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import com.auditlogservice.dto.AuditEventQueryResponse;
import com.auditlogservice.dto.AuditEventRequest;
import com.auditlogservice.dto.AuditEventResponse;
import com.auditlogservice.dto.AuditVerificationIssue;
import com.auditlogservice.repository.AuditRecordRepository;
import com.auditlogservice.service.AuditChainHasher;
import com.auditlogservice.service.AuditLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuditLogServiceRepositoryIntegrationTest {

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Autowired
    private ObjectMapper objectMapper;

        @Autowired
        private AuditChainHasher auditChainHasher;

    @BeforeEach
    void clearData() {
        auditRecordRepository.deleteAll();
    }

    @Test
    void appendQueryAndVerifyAgainstRepositoryBackedDatabase() throws Exception {
        AuditEventRequest firstRequest = request("USER_LOGIN", "actor-1", "ACCOUNT", "acct-1",
                objectMapper.readTree("{\"ip\":\"127.0.0.1\"}"), OffsetDateTime.parse("2026-08-05T10:15:30Z"));
        AuditEventRequest secondRequest = request("RECORD_UPDATED", "actor-1", "ACCOUNT", "acct-1",
                objectMapper.readTree("{\"field\":\"address\"}"), OffsetDateTime.parse("2026-08-05T10:16:30Z"));

        AuditEventResponse firstResponse = auditLogService.append(firstRequest);
        AuditEventResponse secondResponse = auditLogService.append(secondRequest);

        assertThat(firstResponse.sequenceNumber()).isEqualTo(1L);
        assertThat(secondResponse.sequenceNumber()).isEqualTo(2L);
        assertThat(secondResponse.prevHash()).isEqualTo(firstResponse.recordHash());

        AuditEventQueryResponse queryResponse = auditLogService.query("actor-1", "ACCOUNT", "acct-1", null,
                null, null, 0, 20);
        assertThat(queryResponse.totalElements()).isEqualTo(2L);
        assertThat(queryResponse.items()).extracting(AuditEventResponse::sequenceNumber)
                .containsExactly(1L, 2L);

        AuditVerificationIssue intactIssue = auditLogService.verify();
        assertThat(intactIssue.intact()).isTrue();
        assertThat(intactIssue.checkedRecords()).isEqualTo(2L);
    }

    @Test
    void verifyDetectsTamperingWhenStoredRecordChanges() throws Exception {
        AuditEventRequest request = request("PERMISSION_GRANTED", "actor-2", "ACCOUNT", "acct-9",
                objectMapper.readTree("{\"scope\":\"read\"}"), OffsetDateTime.parse("2026-08-05T11:00:00Z"));

        AuditEventResponse response = auditLogService.append(request);
        assertThat(response.sequenceNumber()).isEqualTo(1L);

        var storedRecord = auditRecordRepository.findAllByOrderBySequenceNumberAsc().getFirst();
        storedRecord.setPayloadJson("{\"scope\":\"write\"}");
        auditRecordRepository.saveAndFlush(storedRecord);

        AuditVerificationIssue verificationIssue = auditLogService.verify();
        assertThat(verificationIssue.intact()).isFalse();
        assertThat(verificationIssue.violationType()).isEqualTo("RECORD_HASH_MISMATCH");
        assertThat(verificationIssue.firstBadSequenceNumber()).isEqualTo(1L);
    }

    private AuditEventRequest request(String eventType,
                                      String actorId,
                                      String resourceType,
                                      String resourceId,
                                      JsonNode payload,
                                      OffsetDateTime timestamp) {
        return new AuditEventRequest(eventType, actorId, resourceType, resourceId, payload, timestamp);
    }
}
