package com.auditlogservice.web;

import java.time.OffsetDateTime;

import com.auditlogservice.dto.AuditEventQueryResponse;
import com.auditlogservice.dto.AuditEventRequest;
import com.auditlogservice.dto.AuditEventResponse;
import com.auditlogservice.dto.AuditVerificationIssue;
import com.auditlogservice.dto.RetentionRunResponse;
import com.auditlogservice.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping("/events")
    public ResponseEntity<AuditEventResponse> append(@Valid @RequestBody AuditEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auditLogService.append(request));
    }

    @GetMapping("/events")
    public ResponseEntity<AuditEventQueryResponse> query(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.query(actorId, resourceType, resourceId, eventType, from, to, includeArchived, page, size));
    }

    @PostMapping("/retention/run")
    public ResponseEntity<RetentionRunResponse> runRetention(@RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(auditLogService.runRetention(days));
    }

    @GetMapping("/verify")
    public ResponseEntity<AuditVerificationIssue> verify() {
        return ResponseEntity.ok(auditLogService.verify());
    }
}
