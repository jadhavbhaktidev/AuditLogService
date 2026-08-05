# Architecture Overview

## Components

1. API Layer
   - Spring MVC controllers
   - Write endpoint (append only)
   - Query endpoint (filters + pagination)
   - Verify endpoint
   - Extension endpoints (retention/redaction/export)
2. Domain Layer
   - Spring services for orchestration and policy enforcement
   - Event validation
   - Canonical serialization
   - Hash chain construction
   - Verification engine
3. Persistence Layer
   - PostgreSQL + Spring Data JPA repositories
   - Append-only event table/store
   - Optional archive table/store
   - Audit metadata for redaction and retention actions
4. Verification Utilities
   - Full-chain verifier
   - Export-bundle verifier

## Concrete Stack

1. Java 21
2. Spring Boot 3.x
3. PostgreSQL 16+
4. Flyway for schema migrations
5. Jackson with deterministic JSON field ordering for canonicalization

## Data Model (Draft)

### audit_records

- sequence_number (monotonic)
- event_type
- actor_id
- resource_type
- resource_id
- payload_json
- event_timestamp
- ingestion_timestamp
- prev_hash
- record_hash
- chain_version
- redaction_state
- archived_at (nullable)
- created_at

### redaction_audit

- id
- sequence_number
- redacted_fields
- redaction_reason
- approved_by
- approved_at
- proof_artifact

## Hash Chain Design

1. Genesis prev_hash constant for first record.
2. Canonical serialization of hash input fields.
3. record_hash = SHA256(canonical_record + prev_hash).
4. Verification re-computes and compares per sequence.
5. First mismatch terminates scan and reports violation type.

## Violation Types

1. PREV_HASH_MISMATCH
2. RECORD_HASH_MISMATCH
3. SEQUENCE_GAP_OR_REORDER
4. MISSING_RECORD
5. INVALID_ARCHIVE_LINKAGE

## Key Trade-Offs

1. Strong tamper evidence vs operational complexity.
2. Redaction privacy needs vs immutable evidence guarantees.
3. Full-chain verification cost vs periodic checkpoint optimization.
4. Simplicity of single-writer ordering vs throughput scaling.

## Security Notes

1. No update/delete API for audit records.
2. High-impact actions require explicit human approval flow.
3. Redaction action itself is auditable.
4. Access to direct datastore mutation is operationally restricted.

## Build and Runtime Notes

1. Single service process, stateless API tier.
2. Database transaction boundaries centered on append operation and sequence allocation.
3. Prefer optimistic locking and unique constraints to protect chain ordering guarantees.
