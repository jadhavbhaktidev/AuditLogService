package com.auditlogservice.dto;

public record AuditVerificationIssue(
        boolean intact,
        Long firstBadSequenceNumber,
        String violationType,
        String message,
        long checkedRecords,
        String expectedValue,
        String actualValue) {
}
