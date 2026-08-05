# Normalized Requirements

Date: 2026-08-05

## Functional Requirements

1. Provide append-only write API for audit events.
2. Store required event fields:
   - eventType
   - actorId
   - resourceType
   - resourceId
   - payload (structured object)
   - timestamp
3. Provide query API with filters by any combination of:
   - actorId
   - resourceType + resourceId
   - eventType
   - time range (`from`, `to`)
4. Support pagination for query responses.
5. Enforce tamper evidence using hash chain:
   - record content hash
   - previous record hash link
6. Provide chain verification endpoint that reports:
   - intact or broken
   - first inconsistent record
   - violation type
7. Support retention/archive behavior without false chain-break alarms.
8. Support structured redaction of sensitive payload fields while preserving tamper-evidence guarantees.
9. Support bulk export for actorId or resourceId including metadata for independent verification.

## Non-Functional Requirements

1. Clean maintainable design with clear module boundaries.
2. Production-style quality gates (lint/tests/security checks).
3. Documented assumptions, risks, and trade-offs.

## Ambiguities and Initial Assumptions

1. Timestamp policy:
   - Assumption: server assigns canonical timestamp; optional caller timestamp preserved separately.
2. Storage choice:
   - Assumption: PostgreSQL is used as the system of record with immutable inserts and indexed query filters.
3. Hash algorithm:
   - Assumption: SHA-256 over canonicalized event payload.
4. Redaction semantics:
   - Assumption: redacted value replaced by deterministic commitment/proof artifact, not raw deletion.

## Out of Scope (Initial)

1. Multi-region replication.
2. External IAM integration.
3. End-user UI.
4. Real-time stream processing.

## Acceptance Baseline

1. End-to-end APIs run locally.
2. Tamper is detected reliably after direct data mutation.
3. Retention and redaction do not cause false integrity results under designed rules.
4. Export bundle can be verified independently.

## Selected Technology Stack

1. Language: Java 21
2. Framework: Spring Boot 3.x
3. Database: PostgreSQL 16+
4. Data access: Spring Data JPA (with explicit SQL where needed)
5. Migrations: Flyway
6. Testing: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL)
7. Build: Maven

## Git Operation Guardrail

1. No commit or push is performed by the assistant without explicit user approval in the current conversation.
