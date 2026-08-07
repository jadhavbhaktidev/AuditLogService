# System Design Document (SDD)

## Document Control

- System: Audit Log Service
- Version: 1.0
- Date: 2026-08-06
- Scope: Scenario A, Scenario B, Scenario C

## 1. System Overview

The system is a Spring Boot service backed by PostgreSQL, implementing append-only audit persistence with SHA-256 hash chaining. It provides query, verification, retention, redaction, export, and compliance reporting APIs.

## 2. Architecture

### 2.1 Logical Layers

1. API Layer
   - Controllers expose REST endpoints.
2. Domain Layer
   - Services implement validation, hash chaining, verification, retention, redaction, export, and reporting logic.
3. Persistence Layer
   - JPA repositories over PostgreSQL tables.
4. Utility Layer
   - Canonical serialization and checksum/verification helpers.

### 2.2 Deployment Topology

1. Stateless service process.
2. PostgreSQL as system of record.
3. Frontend console via Vite for local interaction/testing.

## 3. Scenario-to-Component Mapping

1. Scenario A
   - Append/query/verify controllers and hash-chain services.
2. Scenario B
   - Retention service, redaction service + redaction audit store, export/verify utilities.
3. Scenario C
   - Compliance reporting service with scoped validation rules.

## 4. Data Model

### 4.1 Primary Table: audit_records

1. sequence_number (monotonic)
2. event_type
3. actor_id
4. resource_type
5. resource_id
6. payload_json
7. event_timestamp
8. ingestion_timestamp
9. prev_hash
10. record_hash
11. chain_version
12. redaction_state
13. archived_at
14. created_at

### 4.2 Supporting Table: redaction_audit

1. id
2. sequence_number
3. redacted_fields
4. redaction_reason
5. approved_by
6. approved_at
7. proof_artifact

### 4.3 Indexing Strategy

1. sequence_number unique index for ordering.
2. actor_id, resource_type/resource_id, event_type, event_timestamp indexes for filtered retrieval.
3. archived_at index to optimize active versus archived filtering.

## 5. API Design

### 5.1 Endpoints

1. POST /audit/events
2. GET /audit/events
3. GET /audit/verify
4. POST /audit/retention/run
5. POST /audit/redactions
6. GET /audit/exports
7. POST /audit/exports/verify
8. GET /audit/compliance/report

### 5.2 Request Validation Rules

1. Write requests require core event fields.
2. Query supports optional filters and pagination bounds.
3. Export requires actorId or resourceType plus resourceId.
4. Compliance report requires from, to, and actorId or resourceId.
5. Invalid combinations return HTTP 400 with structured request error.

## 6. Core Algorithms

### 6.1 Canonicalization

1. Normalize hash input fields with deterministic ordering.
2. Persist timestamp in deterministic form (epoch-millis normalization path in implementation).

### 6.2 Hash Chain Construction

1. Determine prev_hash from latest sequence record.
2. Compute record_hash as SHA-256 over canonical record fields plus prev_hash.
3. Persist new row atomically with sequence allocation.

### 6.3 Chain Verification

1. Scan records by sequence order.
2. Recompute prev-hash continuity and record_hash.
3. Stop at first mismatch and return violation details.

Violation types:

1. PREV_HASH_MISMATCH
2. RECORD_HASH_MISMATCH
3. SEQUENCE_GAP_OR_REORDER
4. MISSING_RECORD
5. INVALID_ARCHIVE_LINKAGE

### 6.4 Redaction Read-Model Masking

1. Store redaction approval and field paths in redaction_audit.
2. Keep original payload immutable in audit_records.
3. Apply masking in query/export/report response payload projection.

### 6.5 Export Bundle Verification

1. Build deterministic bundle checksum across metadata and records.
2. Validate checksum and structural metadata at verify time.
3. Reuse integrity context to report validity and violations.

## 7. Sequence Flows

### 7.1 Append Event Flow

1. API validate request.
2. Domain canonicalize and hash.
3. Persist immutable record with sequence and hashes.
4. Return append result.

### 7.2 Query and Filter Flow

1. API parse filter combinations and pagination.
2. Repository fetch ordered rows.
3. Domain projects redaction-safe payload.
4. Return paged result.

### 7.3 Retention Flow

1. Retention API resolves cutoff days.
2. Repository marks older rows as archived.
3. Verify path remains chain-complete across archived rows.

### 7.4 Compliance Report Flow

1. Validate from/to and scope requirements.
2. Enforce ACCOUNT resource scope.
3. Fetch paged rows and apply read masking.
4. Compute/attach integrity summary.

## 8. Error Model

1. Request validation failures return REQUEST_ERROR payload.
2. Domain errors include clear messages for filter/scope issues.
3. Verify responses always include integrity status, with mismatch context when broken.

## 9. Security and Governance Controls

1. No update/delete APIs for audit_records.
2. Redaction requires approvedBy and reason metadata.
3. Redaction action is itself auditable.
4. Datastore direct mutation is operationally restricted outside API.

## 10. Quality, Testing, and CI

1. Unit tests for hashing/canonicalization/verification logic.
2. Integration tests for all scenario flows and tamper conditions.
3. Maven verify gate includes Checkstyle and SpotBugs.
4. Security scan via OWASP Dependency Check profile.
5. CI executes verification and dependency scan workflows.

## 11. Performance and Scalability Notes

1. Full-chain verification is O(n) and can become expensive for very large datasets.
2. Pagination protects query response size and memory usage.
3. Future optimization path: verification checkpoints and partitioned archival.

## 12. Trade-Offs and Future Design Evolution

1. Immutable storage plus read-time masking preserves evidence but retains sensitive source values.
2. Marker-based archival simplifies implementation but does not reduce primary-table size.
3. Bundle checksum improves tamper detection but does not provide signer identity; signed artifacts are future scope.

## 13. Operational Runbook Essentials

1. Health checks via actuator and service health endpoints.
2. Verify endpoint can be used as operational integrity control.
3. Retention runs should be scheduled and monitored.
4. Redaction and export actions should be access-controlled and audit-reviewed.
