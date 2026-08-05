package com.auditlogservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.auditlogservice.repository.AuditRecordRepository;
import com.auditlogservice.repository.RedactionAuditRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

        @Autowired
        private RedactionAuditRepository redactionAuditRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearData() {
                redactionAuditRepository.deleteAll();
        auditRecordRepository.deleteAll();
    }

    @Test
    void appendQueryAndVerifyEndpointsWork() throws Exception {
        String firstEvent = """
                {
                  "eventType": "USER_LOGIN",
                  "actorId": "actor-1",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-1",
                  "payload": {"ip": "127.0.0.1"},
                  "timestamp": "2026-08-05T10:15:30Z"
                }
                """;

        String secondEvent = """
                {
                  "eventType": "RECORD_UPDATED",
                  "actorId": "actor-1",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-1",
                  "payload": {"field": "address"},
                  "timestamp": "2026-08-05T10:16:30Z"
                }
                """;

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstEvent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sequenceNumber").value(1));

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondEvent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sequenceNumber").value(2));

        mockMvc.perform(get("/audit/events")
                        .param("actorId", "actor-1")
                        .param("resourceType", "ACCOUNT")
                        .param("resourceId", "acct-1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].sequenceNumber").value(1))
                .andExpect(jsonPath("$.items[1].sequenceNumber").value(2));

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.checkedRecords").value(2));
    }

    @Test
    void appendValidationFailureReturnsStructuredError() throws Exception {
        String invalidEvent = """
                {
                  "eventType": "",
                  "actorId": "actor-1",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-1",
                  "payload": {"ip": "127.0.0.1"}
                }
                """;

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidEvent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields").isArray());
    }

    @Test
    void verifyEndpointDetectsTampering() throws Exception {
        String event = """
                {
                  "eventType": "PERMISSION_GRANTED",
                  "actorId": "actor-2",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-9",
                  "payload": {"scope": "read"},
                  "timestamp": "2026-08-05T11:00:00Z"
                }
                """;

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event))
                .andExpect(status().isCreated());

        var storedRecord = auditRecordRepository.findAllByOrderBySequenceNumberAsc().getFirst();
        storedRecord.setPayloadJson("{\"scope\":\"write\"}");
        auditRecordRepository.saveAndFlush(storedRecord);

        String response = mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(false))
                .andExpect(jsonPath("$.firstBadSequenceNumber").value(1))
                .andExpect(jsonPath("$.violationType").value("RECORD_HASH_MISMATCH"))
                .andExpect(jsonPath("$.expectedValue").isString())
                .andExpect(jsonPath("$.actualValue").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        assertThat(node.get("expectedValue").asText()).isNotEqualTo(node.get("actualValue").asText());
    }

    @Test
    void retentionArchivesOlderRecordsWithoutBreakingVerification() throws Exception {
        String oldEvent = """
                {
                  "eventType": "USER_LOGIN",
                  "actorId": "actor-r",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-r",
                  "payload": {"ip": "10.0.0.1"},
                  "timestamp": "2020-01-01T00:00:00Z"
                }
                """;

        String newEvent = """
                {
                  "eventType": "USER_LOGIN",
                  "actorId": "actor-r",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-r",
                  "payload": {"ip": "10.0.0.2"},
                  "timestamp": "2030-01-01T00:00:00Z"
                }
                """;

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oldEvent))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newEvent))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/audit/retention/run")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedCount").value(1))
                .andExpect(jsonPath("$.retentionDays").value(30));

        mockMvc.perform(get("/audit/events")
                        .param("actorId", "actor-r")
                        .param("resourceType", "ACCOUNT")
                        .param("resourceId", "acct-r")
                        .param("includeArchived", "false")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/audit/events")
                        .param("actorId", "actor-r")
                        .param("resourceType", "ACCOUNT")
                        .param("resourceId", "acct-r")
                        .param("includeArchived", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.checkedRecords").value(2));
    }

    @Test
    void redactionMasksFieldsInQueryWithoutBreakingVerification() throws Exception {
        String event = """
                {
                  "eventType": "USER_UPDATED",
                  "actorId": "actor-z",
                  "resourceType": "ACCOUNT",
                  "resourceId": "acct-z",
                  "payload": {
                    "email": "user@example.com",
                    "token": "tok-secret",
                    "nested": {"ssn": "111-22-3333", "city": "Pune"}
                  },
                  "timestamp": "2026-08-05T12:00:00Z"
                }
                """;

        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sequenceNumber").value(1));

        String redaction = """
                {
                  "sequenceNumber": 1,
                  "redactedFields": ["token", "nested.ssn"],
                  "reason": "GDPR request",
                  "approvedBy": "compliance-1"
                }
                """;

        mockMvc.perform(post("/audit/redactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(redaction))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequenceNumber").value(1))
                .andExpect(jsonPath("$.redactionState").value("REDACTED"))
                .andExpect(jsonPath("$.redactedFields[0]").value("token"))
                .andExpect(jsonPath("$.redactedFields[1]").value("nested.ssn"));

        mockMvc.perform(get("/audit/events")
                        .param("actorId", "actor-z")
                        .param("resourceType", "ACCOUNT")
                        .param("resourceId", "acct-z")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].payload.email").value("user@example.com"))
                .andExpect(jsonPath("$.items[0].payload.token").value("[REDACTED]"))
                .andExpect(jsonPath("$.items[0].payload.nested.ssn").value("[REDACTED]"))
                .andExpect(jsonPath("$.items[0].payload.nested.city").value("Pune"));

        var storedRecord = auditRecordRepository.findBySequenceNumber(1L).orElseThrow();
        JsonNode persistedPayload = objectMapper.readTree(storedRecord.getPayloadJson());
        assertThat(persistedPayload.get("token").asText()).isEqualTo("tok-secret");
        assertThat(persistedPayload.get("nested").get("ssn").asText()).isEqualTo("111-22-3333");

        mockMvc.perform(get("/audit/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.checkedRecords").value(1));
    }
}
