# Implementation Backlog (Issue-Ready)

## Epic 0 - Repo and Process Guardrails

- [ ] BL-001 Create private repo and baseline scaffold.
- [ ] BL-002 Add required attestation.
- [ ] BL-003 Start AI traceability log.
- [ ] BL-004 Define commit cadence and review checkpoints.
- [ ] BL-005 Enforce workflow guardrail: require explicit user approval before any commit/push.

## Epic 0.1 - Stack Bootstrap (Java/Spring/PostgreSQL)

- [ ] BL-010 Initialize Spring Boot project (Java 21, Maven).
- [ ] BL-011 Add dependencies: Web, Validation, Data JPA, Actuator, Flyway, PostgreSQL driver.
- [ ] BL-012 Create local profile config and DB connection properties.
- [ ] BL-013 Add Docker Compose for local PostgreSQL.
- [ ] BL-014 Add baseline health endpoint and app startup verification test.

## Epic 1 - Scenario A Core Service

- [ ] BL-101 Define write/query/verify API contracts.
- [ ] BL-102 Implement request DTOs and Bean Validation constraints.
- [ ] BL-103 Implement deterministic canonical JSON serializer (Jackson config).
- [ ] BL-104 Create Flyway migration for audit tables and indexes.
- [ ] BL-105 Implement JPA entities/repositories and append-only sequence allocation.
- [ ] BL-106 Implement hash chain insertion service (SHA-256).
- [ ] BL-107 Implement query specifications and cursor/page pagination.
- [ ] BL-108 Implement `/audit/verify` full-chain verification endpoint.
- [ ] BL-109 Add tamper simulation integration test against PostgreSQL.
- [ ] BL-110 Add API exception handling and validation tests.

## Epic 2 - Scenario B Extension

- [ ] BL-201 Implement retention policy configuration.
- [ ] BL-202 Implement archive workflow and metadata.
- [ ] BL-203 Update verifier for legitimate archive handling.
- [x] BL-204 Implement structured redaction strategy.
- [x] BL-205 Add redaction approval/audit metadata.
- [x] BL-206 Implement bulk export endpoint.
- [x] BL-207 Implement offline bundle verifier utility.
- [x] BL-208 Add retention/redaction/export integration tests.

## Epic 3 - Scenario C Ambiguous Requirement

- [x] BL-301 Write clarification and assumptions document.
- [x] BL-302 Translate to concrete report API requirements.
- [x] BL-303 Implement scoped compliance reporting endpoint.
- [x] BL-304 Add compliance report tests and fixtures.
- [x] BL-305 Document deferred scope and rationale.

## Epic 4 - Quality Gates and Documentation

- [ ] BL-401 Add lint and static analysis pipeline (Checkstyle + SpotBugs).
- [ ] BL-402 Add CI test workflow.
- [ ] BL-403 Add dependency security scan (OWASP Dependency Check).
- [ ] BL-404 Finalize architecture and testing docs.
- [ ] BL-405 Finalize final engineering summary.
- [ ] BL-406 Rehearse live defense demo flow.

## Suggested Execution Order

1. BL-001 through BL-004
2. BL-005 through BL-014
3. BL-101 through BL-110
4. BL-201 through BL-208
5. BL-301 through BL-305
6. BL-401 through BL-406
