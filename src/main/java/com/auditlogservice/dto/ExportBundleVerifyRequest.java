package com.auditlogservice.dto;

import jakarta.validation.constraints.NotNull;

public record ExportBundleVerifyRequest(@NotNull ExportBundleResponse bundle) {
}
