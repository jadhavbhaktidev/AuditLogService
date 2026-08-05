package com.auditlogservice.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.auditlogservice.domain.AuditRecord;
import com.auditlogservice.dto.AuditEventQueryResponse;
import com.auditlogservice.dto.AuditEventRequest;
import com.auditlogservice.dto.AuditEventResponse;
import com.auditlogservice.dto.AuditVerificationIssue;
import com.auditlogservice.dto.RetentionRunResponse;
import com.auditlogservice.repository.AuditRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final AuditChainHasher auditChainHasher;
    private final AuditChainVerifier auditChainVerifier;
    private final ObjectMapper objectMapper;
    private final int defaultRetentionDays;

    public AuditLogService(AuditRecordRepository auditRecordRepository,
                           AuditChainHasher auditChainHasher,
                           AuditChainVerifier auditChainVerifier,
                           ObjectMapper objectMapper,
                           @Value("${audit.retention.days:365}") int defaultRetentionDays) {
        this.auditRecordRepository = auditRecordRepository;
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
        return toResponse(saved);
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
        List<AuditEventResponse> items = pageResult.stream().map(this::toResponse).toList();
        return new AuditEventQueryResponse(items, pageResult.getNumber(), pageResult.getSize(), pageResult.getTotalElements(), pageResult.getTotalPages());
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

    private AuditEventResponse toResponse(AuditRecord record) {
        return new AuditEventResponse(
                record.getId(),
                record.getSequenceNumber(),
                record.getEventType(),
                record.getActorId(),
                record.getResourceType(),
                record.getResourceId(),
                parsePayload(record.getPayloadJson()),
                OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(record.getEventTimestamp()), ZoneOffset.UTC),
                record.getPrevHash(),
                record.getRecordHash());
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
