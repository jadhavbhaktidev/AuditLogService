# Product Design Document (PDD)

## Document Control

- Product: Audit Log Service
- Version: 1.0
- Date: 2026-08-06
- Scope: Scenario A, Scenario B, Scenario C

## 1. Product Vision

Deliver a practical, operator-friendly, and compliance-ready audit platform that combines immutable evidence semantics with privacy and governance controls.

## 2. Target Users and Personas

1. Security Analyst
   - Needs rapid, filterable search and confidence in data integrity.
2. Compliance Reviewer
   - Needs bounded reports and proof of evidence trustworthiness.
3. Privacy Approver
   - Needs auditable redaction workflows and approval traceability.
4. Platform Operator
   - Needs predictable API behavior and straightforward operational checks.

## 3. Product Scope and Modules

1. Core Audit Logging (Scenario A)
2. Governance Extensions (Scenario B)
3. Compliance Reporting (Scenario C)
4. Developer/Operator Console (frontend)

## 4. End-to-End User Journeys

### 4.1 Journey A: Capture and Verify Events

1. User posts an audit event.
2. System validates request and appends immutable record.
3. User queries records with filters and pagination.
4. User verifies global chain integrity.

Expected outcomes:

1. No mutation endpoints for existing records.
2. Verify response confirms intact or reports first failure.

### 4.2 Journey B: Govern Data Lifecycle and Export Evidence

1. Operator runs retention process.
2. Archived records are hidden unless includeArchived is true.
3. Privacy approver submits redaction request with metadata.
4. Investigator exports scoped bundle.
5. Investigator verifies bundle integrity offline or via API.

Expected outcomes:

1. Integrity is preserved post-archive and post-redaction.
2. Redacted fields are masked in read paths.
3. Tampered bundles fail verification.

### 4.3 Journey C: Produce Compliance Report

1. Compliance user submits report request with from/to plus actor or resource scope.
2. System enforces ACCOUNT resource scope.
3. System returns paginated report with integrity snapshot.

Expected outcomes:

1. Invalid scope combinations are rejected with clear error.
2. Report is audit-ready for prototype compliance workflows.

## 5. Functional Requirements by Module

### 5.1 Scenario A Functional Requirements

1. Event append API accepts eventType, actorId, resourceType, resourceId, payload, timestamp.
2. Query API supports any filter combination and pagination.
3. Verify API returns intact flag and first failure diagnostics.

### 5.2 Scenario B Functional Requirements

1. Retention run API archives records older than policy window.
2. Query API supports includeArchived boolean.
3. Redaction API accepts sequenceNumber, fields, reason, approvedBy.
4. Export API supports actor scope or resource scope.
5. Export verify API validates checksum and chain context.

### 5.3 Scenario C Functional Requirements

1. Compliance report API requires from and to bounds.
2. Compliance report API requires actorId or resourceId.
3. Response contains report metadata, items, and chain integrity summary.

## 6. API Product Contracts

1. POST /audit/events
2. GET /audit/events
3. GET /audit/verify
4. POST /audit/retention/run
5. POST /audit/redactions
6. GET /audit/exports
7. POST /audit/exports/verify
8. GET /audit/compliance/report

## 7. UX and Interaction Design (Frontend Console)

### 7.1 Primary Screens/Sections

1. Create Event
2. Query Events
3. Verify Chain
4. Retention and Redaction
5. Verify Export Bundle

### 7.2 Key Interaction Rules

1. Query filters are dropdown-based for actor/resource/event and combinable with date range.
2. Export action is triggered from Query section using active filters.
3. Export section is verify-only (bundle input plus verify action).
4. Query table emphasizes operational readability (event, actor, resource, timestamp).

## 8. Data and Privacy Behavior

1. Immutable storage is preserved for tamper evidence.
2. Redaction is read-model masking, not hash-source mutation.
3. Proof artifacts store commitments and approval context.
4. Archived records remain verifiable and retrievable when explicitly requested.

## 9. Error Handling and UX Feedback

1. Validation failures return structured request error with human-readable message.
2. Frontend status bar reflects latest operation success/failure state.
3. Verify and export verify sections present concise integrity outcomes.

## 10. Non-Functional Product Requirements

1. Determinism: canonical serialization and stable hash behavior.
2. Performance: paginated retrieval for high-volume datasets.
3. Security: no write-back mutation APIs for audit records.
4. Quality: CI checks and security scan gates are mandatory.

## 11. Product Acceptance Test Matrix

1. Scenario A
   - append/query/verify happy path
   - tamper detection
   - mixed filter queries and pagination
2. Scenario B
   - retention with verify intact
   - redaction masking behavior
   - export and tampered bundle verification
3. Scenario C
   - required parameter validation
   - account-scope filtering
   - integrity summary presence

## 12. Release Readiness Criteria

1. All scenario integration tests pass.
2. API contracts align with scenario docs.
3. Frontend console supports all primary operator journeys.
4. Quality gates pass in CI.

## 13. Future Enhancements

1. Signed export artifacts for non-repudiation.
2. Jurisdiction-specific report formats (CSV/Parquet/templates).
3. Incremental verification/checkpoint optimization.
4. Policy-driven redaction authorization integration.
