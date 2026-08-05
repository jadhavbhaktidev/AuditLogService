package com.auditlogservice.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.auditlogservice.domain.AuditRecord;
import com.auditlogservice.domain.RedactionAudit;
import com.auditlogservice.dto.AuditEventQueryResponse;
import com.auditlogservice.dto.AuditEventRequest;
import com.auditlogservice.dto.AuditEventResponse;
import com.auditlogservice.dto.AuditVerificationIssue;
import com.auditlogservice.dto.RedactionRequest;
import com.auditlogservice.dto.RedactionResponse;
import com.auditlogservice.dto.RetentionRunResponse;
import com.auditlogservice.repository.AuditRecordRepository;
import com.auditlogservice.repository.RedactionAuditRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditLogService {

    private final AuditRecordRepository auditRecordRepository;
    private final RedactionAuditRepository redactionAuditRepository;
    private final AuditChainHasher auditChainHasher;
    private final AuditChainVerifier auditChainVerifier;
    private final ObjectMapper objectMapper;
    private final int defaultRetentionDays;

    public AuditLogService(AuditRecordRepository auditRecordRepository,
                           RedactionAuditRepository redactionAuditRepository,
                           AuditChainHasher auditChainHasher,
                           AuditChainVerifier auditChainVerifier,
                           ObjectMapper objectMapper,
                           @Value("${audit.retention.days:365}") int defaultRetentionDays) {
        this.auditRecordRepository = auditRecordRepository;
        this.redactionAuditRepository = redactionAuditRepository;
        this.auditChainHasher = auditChainHasher;
        this.auditChainVerifier = auditChainVerifier;
        this.objectMapper = objectMapper;
        this.defaultRetentionDays = defaultRetentionDays;
    }

    @Transactional
    public AuditEventResponse append(AuditEventRequest request) {
        OffsetDateTime resolvedTimestamp = request.timestamp() != null ? request.timestamp() : OffsetDateTime.now(ZoneOffset.UTC);
        long eventTimestamp = resolvedTimestamp.toInstant().toEpochMilli();
        Long maxSequence = auditRecordRepository.findMaxSequenceNumber();
        long nextSequence = maxSequence + 1;
        String prevHash = maxSequence == 0
                ? AuditChainHasher.GENESIS_PREV_HASH
                : auditRecordRepository.findAllByOrderBySequenceNumberAsc().getLast().getRecordHash();
        String canonicalPayload = auditChainHasher.canonicalPayload(request, eventTimestamp, prevHash);
        String recordHash = auditChainHasher.sha256Hex(canonicalPayload);

        AuditRecord record = new AuditRecord();
        record.setSequenceNumber(nextSequence);
        record.setEventType(request.eventType());
        record.setActorId(request.actorId());
        record.setResourceType(request.resourceType());
        record.setResourceId(request.resourceId());
        record.setPayloadJson(serializePayload(request.payload()));
        record.setEventTimestamp(eventTimestamp);
        record.setIngestionTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        record.setPrevHash(prevHash);
        record.setRecordHash(recordHash);
        record.setChainVersion(1);
        record.setRedactionState("NONE");
        record.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        AuditRecord saved = auditRecordRepository.save(record);
        return toResponse(saved, null);
    }

    public AuditEventQueryResponse query(String actorId,
                                         String resourceType,
                                         String resourceId,
                                         String eventType,
                                         OffsetDateTime from,
                                         OffsetDateTime to,
                                         boolean includeArchived,
                                         int page,
                                         int size) {
        Specification<AuditRecord> specification = Specification.where(null);
        if (StringUtils.hasText(actorId)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("actorId"), actorId));
        }
        if (StringUtils.hasText(resourceType)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("resourceType"), resourceType));
        }
        if (StringUtils.hasText(resourceId)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("resourceId"), resourceId));
        }
        if (StringUtils.hasText(eventType)) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("eventType"), eventType));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("eventTimestamp"), from.toInstant().toEpochMilli()));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("eventTimestamp"), to.toInstant().toEpochMilli()));
        }
        if (!includeArchived) {
            specification = specification.and((root, query, builder) -> builder.isNull(root.get("archivedAt")));
        }

        var pageResult = auditRecordRepository.findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sequenceNumber")));
        Map<Long, RedactionAudit> redactionBySequence = latestRedactionsBySequence(pageResult.getContent());
        List<AuditEventResponse> items = pageResult.stream()
                .map(record -> toResponse(record, redactionBySequence.get(record.getSequenceNumber())))
                .toList();
        return new AuditEventQueryResponse(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages());
    }

    @Transactional
    public RedactionResponse redact(RedactionRequest request) {
        AuditRecord record = auditRecordRepository.findBySequenceNumber(request.sequenceNumber())
                .orElseThrow(() -> new IllegalArgumentException("Sequence number not found: " + request.sequenceNumber()));

        List<String> normalizedFields = normalizeRedactedFields(request.redactedFields());
        JsonNode payloadNode = parsePayload(record.getPayloadJson());
        if (!(payloadNode instanceof ObjectNode payloadObject)) {
            throw new IllegalArgumentException("Only object payloads are supported for structured redaction");
        }

        ObjectNode commitments = objectMapper.createObjectNode();
        for (String fieldPath : normalizedFields) {
            JsonNode fieldValue = readField(payloadObject, fieldPath);
            if (fieldValue == null) {
                throw new IllegalArgumentException("Redaction field path not found: " + fieldPath);
            }
            commitments.put(fieldPath, auditChainHasher.sha256Hex(fieldPath + ":" + fieldValue.toString()));
        }

        OffsetDateTime approvedAt = OffsetDateTime.now(ZoneOffset.UTC);
        ObjectNode proofNode = objectMapper.createObjectNode();
        proofNode.put("sequenceNumber", request.sequenceNumber());
        proofNode.put("approvedBy", request.approvedBy());
        proofNode.put("approvedAt", approvedAt.toString());
        ArrayNode fieldsNode = proofNode.putArray("fields");
        normalizedFields.forEach(fieldsNode::add);
        proofNode.set("commitments", commitments);

        RedactionAudit redactionAudit = new RedactionAudit();
        redactionAudit.setSequenceNumber(request.sequenceNumber());
        redactionAudit.setRedactedFieldsJson(writeValue(normalizedFields, "Unable to serialize redaction fields"));
        redactionAudit.setRedactionReason(request.reason());
        redactionAudit.setApprovedBy(request.approvedBy());
        redactionAudit.setApprovedAt(approvedAt);
        redactionAudit.setProofArtifact(writeValue(proofNode, "Unable to serialize redaction proof artifact"));
        redactionAudit.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        redactionAuditRepository.save(redactionAudit);

        record.setRedactionState("REDACTED");
        auditRecordRepository.save(record);

        return new RedactionResponse(
                record.getSequenceNumber(),
                normalizedFields,
                request.reason(),
                request.approvedBy(),
                approvedAt,
                redactionAudit.getProofArtifact(),
                record.getRedactionState());
    }

    @Transactional
    public RetentionRunResponse runRetention(Integer daysOverride) {
        int retentionDays = daysOverride != null ? daysOverride : defaultRetentionDays;
        if (retentionDays < 1) {
            throw new IllegalStateException("Retention days must be greater than zero");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long cutoffEpochMillis = now.minusDays(retentionDays).toInstant().toEpochMilli();
        int archivedCount = auditRecordRepository.archiveOlderThan(cutoffEpochMillis, now);
        return new RetentionRunResponse(archivedCount, retentionDays, cutoffEpochMillis, now);
    }

    public AuditVerificationIssue verify() {
        return auditChainVerifier.verify(auditRecordRepository.findAllByOrderBySequenceNumberAsc());
    }

    private AuditEventResponse toResponse(AuditRecord record, RedactionAudit redactionAudit) {
        JsonNode payload = parsePayload(record.getPayloadJson()).deepCopy();
        if (redactionAudit != null) {
            for (String fieldPath : parseRedactedFields(redactionAudit.getRedactedFieldsJson())) {
                maskField(payload, fieldPath);
            }
        }

        return new AuditEventResponse(
                record.getId(),
                record.getSequenceNumber(),
                record.getEventType(),
                record.getActorId(),
                record.getResourceType(),
                record.getResourceId(),
                payload,
                OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(record.getEventTimestamp()), ZoneOffset.UTC),
                record.getPrevHash(),
                record.getRecordHash());
    }

    private Map<Long, RedactionAudit> latestRedactionsBySequence(List<AuditRecord> records) {
        if (records.isEmpty()) {
            return Map.of();
        }

        List<Long> sequenceNumbers = records.stream().map(AuditRecord::getSequenceNumber).toList();
        List<RedactionAudit> audits = redactionAuditRepository
                .findBySequenceNumberInOrderBySequenceNumberAscApprovedAtDesc(sequenceNumbers);
        Map<Long, RedactionAudit> latestBySequence = new HashMap<>();
        for (RedactionAudit audit : audits) {
            latestBySequence.putIfAbsent(audit.getSequenceNumber(), audit);
        }
        return latestBySequence;
    }

    private List<String> normalizeRedactedFields(List<String> redactedFields) {
        Set<String> uniqueFields = new LinkedHashSet<>();
        for (String fieldPath : redactedFields) {
            String normalized = fieldPath == null ? null : fieldPath.trim();
            if (!StringUtils.hasText(normalized)) {
                throw new IllegalArgumentException("Redaction field path must be non-empty");
            }
            uniqueFields.add(normalized);
        }
        return new ArrayList<>(uniqueFields);
    }

    private List<String> parseRedactedFields(String redactedFieldsJson) {
        try {
            JsonNode node = objectMapper.readTree(redactedFieldsJson);
            List<String> fields = new ArrayList<>();
            for (JsonNode fieldNode : node) {
                fields.add(fieldNode.asText());
            }
            return fields;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse stored redaction fields", exception);
        }
    }

    private JsonNode readField(JsonNode payload, String fieldPath) {
        JsonNode current = payload;
        for (String segment : fieldPath.split("\\.")) {
            if (!current.isObject()) {
                return null;
            }
            current = current.get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void maskField(JsonNode payload, String fieldPath) {
        if (!(payload instanceof ObjectNode objectNode)) {
            return;
        }

        String[] segments = fieldPath.split("\\.");
        ObjectNode currentObject = objectNode;
        for (int index = 0; index < segments.length - 1; index++) {
            JsonNode child = currentObject.get(segments[index]);
            if (!(child instanceof ObjectNode childObject)) {
                return;
            }
            currentObject = childObject;
        }

        String leafField = segments[segments.length - 1];
        if (currentObject.has(leafField)) {
            currentObject.put(leafField, "[REDACTED]");
        }
    }

    private String writeValue(Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(errorMessage, exception);
        }
    }

    private String serializePayload(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize event payload", exception);
        }
    }

    private JsonNode parsePayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse stored payload", exception);
        }
    }
}
