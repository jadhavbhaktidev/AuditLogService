package com.auditlogservice.dto;

public record ExportBundleVerificationResponse(
        boolean valid,
        String message,
        String violationType,
        long checkedRecords,
        String expectedValue,
        String actualValue) {
}
