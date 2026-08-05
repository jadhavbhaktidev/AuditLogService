package com.auditlogservice.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "redaction_audit")
public class RedactionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "redaction_audit_seq")
    @SequenceGenerator(name = "redaction_audit_seq", sequenceName = "redaction_audit_seq", allocationSize = 1)
    private Long id;

    @Column(name = "sequence_number", nullable = false)
    private Long sequenceNumber;

    @Column(name = "redacted_fields", nullable = false, columnDefinition = "text")
    private String redactedFieldsJson;

    @Column(name = "redaction_reason", nullable = false, length = 512)
    private String redactionReason;

    @Column(name = "approved_by", nullable = false, length = 255)
    private String approvedBy;

    @Column(name = "approved_at", nullable = false)
    private OffsetDateTime approvedAt;

    @Column(name = "proof_artifact", nullable = false, columnDefinition = "text")
    private String proofArtifact;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getRedactedFieldsJson() {
        return redactedFieldsJson;
    }

    public void setRedactedFieldsJson(String redactedFieldsJson) {
        this.redactedFieldsJson = redactedFieldsJson;
    }

    public String getRedactionReason() {
        return redactionReason;
    }

    public void setRedactionReason(String redactionReason) {
        this.redactionReason = redactionReason;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getProofArtifact() {
        return proofArtifact;
    }

    public void setProofArtifact(String proofArtifact) {
        this.proofArtifact = proofArtifact;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
