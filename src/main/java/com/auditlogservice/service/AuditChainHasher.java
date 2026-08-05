package com.auditlogservice.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.auditlogservice.dto.AuditEventRequest;
import com.auditlogservice.domain.AuditRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class AuditChainHasher {

    public static final String GENESIS_PREV_HASH = "GENESIS";

    private final ObjectMapper objectMapper;

    public AuditChainHasher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String canonicalPayload(AuditEventRequest request, long resolvedTimestampEpochMillis, String prevHash) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("actorId", request.actorId());
        node.put("eventType", request.eventType());
        node.put("prevHash", prevHash);
        node.put("resourceId", request.resourceId());
        node.put("resourceType", request.resourceType());
        node.put("timestamp", Instant.ofEpochMilli(resolvedTimestampEpochMillis).toString());
        node.set("payload", request.payload());
        return serialize(node);
    }

    public String canonicalPayload(AuditRecord record) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("actorId", record.getActorId());
        node.put("eventType", record.getEventType());
        node.put("prevHash", record.getPrevHash());
        node.put("resourceId", record.getResourceId());
        node.put("resourceType", record.getResourceType());
        node.put("timestamp", Instant.ofEpochMilli(record.getEventTimestamp()).toString());
        node.set("payload", readTree(record.getPayloadJson()));
        return serialize(node);
    }

    public String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String serialize(ObjectNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize canonical audit payload", exception);
        }
    }

    private com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse payload JSON", exception);
        }
    }
}
