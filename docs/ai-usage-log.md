# AI Usage Log / Traceability

Record every meaningful AI interaction used in this project.

## Entry Template

- Timestamp:
- Task ID:
- Prompt Intent:
- Constraints Provided:
- AI Output Summary:
- Disposition: ACCEPTED | MODIFIED | REJECTED
- Human Rationale:
- Validation Performed:
- Follow-up Actions:

## Entries

- 2026-08-05:
  - Task ID: PLAN-INIT
  - Prompt Intent: Generate execution plan and repository blueprint.
  - Constraints Provided: Preserve engineer ownership and traceability.
  - AI Output Summary: Produced implementation plan and docs scaffold.
  - Disposition: MODIFIED
  - Human Rationale: Plan accepted; technical stack details pending manual decisions.
  - Validation Performed: Manual review of coverage against assignment requirements.
  - Follow-up Actions: Select stack and begin Scenario A implementation.

- 2026-08-05:
  - Task ID: STACK-DECISION
  - Prompt Intent: Lock the implementation stack and update execution docs.
  - Constraints Provided: Use Java, Spring Boot, PostgreSQL; do not commit or push without explicit approval.
  - AI Output Summary: Updated requirements, architecture, setup, and backlog to Spring Boot/PostgreSQL-specific tasks.
  - Disposition: ACCEPTED
  - Human Rationale: Aligns plan with chosen implementation path and governance expectation.
  - Validation Performed: Cross-check of updated docs against assignment scenario requirements.
  - Follow-up Actions: Scaffold Spring Boot service and begin Scenario A implementation tasks.

- 2026-08-05:
  - Task ID: SCAFFOLD-BOOTSTRAP
  - Prompt Intent: Create the actual Spring Boot project structure for BL-010 through BL-014.
  - Constraints Provided: Java 21, Spring Boot, PostgreSQL, and no commit or push without approval.
  - AI Output Summary: Added Maven build, package structure, Spring Boot application entrypoint, health endpoint, Flyway baseline migration, PostgreSQL docker-compose, and smoke test.
  - Disposition: ACCEPTED
  - Human Rationale: Provides a compile-ready scaffold that matches the selected implementation stack.
  - Validation Performed: Maven compile check completed without visible errors.
  - Follow-up Actions: Implement Scenario A domain, persistence, and API contracts.

- 2026-08-05:
  - Task ID: SCAFFOLD-JACKSON
  - Prompt Intent: Add deterministic Jackson configuration for future canonical serialization.
  - Constraints Provided: Keep bootstrap changes minimal and compile-safe.
  - AI Output Summary: Added Spring Boot Jackson customizer and validated compile success.
  - Disposition: ACCEPTED
  - Human Rationale: Supports later hash-chain implementation without changing behavior broadly.
  - Validation Performed: `mvn -q -DskipTests compile` returned exit code 0.
  - Follow-up Actions: Use this configuration when implementing Scenario A canonical hashing.

- 2026-08-05:
  - Task ID: SCENARIO-A-SLICE-1
  - Prompt Intent: Implement the first Scenario A slice covering write/query/verify plumbing.
  - Constraints Provided: Keep the work coherent, compile-safe, and test-backed.
  - AI Output Summary: Added audit record entity, repository, DTOs, hash/verifier services, REST controller, exception handler, and focused tests.
  - Disposition: ACCEPTED
  - Human Rationale: Establishes the core tamper-evident path and API surface for later expansion.
  - Validation Performed: `mvn test` passed after fixing the smoke test context with a mocked repository.
  - Follow-up Actions: Add persistence-backed query integration tests and refine verification diagnostics.

- 2026-08-05:
  - Task ID: SCENARIO-A-SLICE-2
  - Prompt Intent: Add repository-backed integration coverage for append/query/verify and tamper detection.
  - Constraints Provided: Keep tests runnable in this environment without requiring Docker.
  - AI Output Summary: Added H2-backed integration test profile, integration tests, and timestamp persistence normalization using epoch millis for deterministic hash verification.
  - Disposition: ACCEPTED
  - Human Rationale: Confirms core behavior with real repository persistence and removes timezone-induced hash drift.
  - Validation Performed: Full `mvn test` suite passed (6 tests).
  - Follow-up Actions: Add API-layer integration tests and refine verify diagnostics payload.

- 2026-08-05:
  - Task ID: SCENARIO-A-SLICE-3
  - Prompt Intent: Add API-layer integration tests and richer verification diagnostics.
  - Constraints Provided: Keep tests deterministic in the local environment and preserve Scenario A API contracts.
  - AI Output Summary: Added controller integration tests for append/query/verify and validation errors; extended verify response with expected/actual mismatch fields.
  - Disposition: ACCEPTED
  - Human Rationale: Improves confidence in externally visible behavior and makes tamper failures easier to interpret.
  - Validation Performed: Full `mvn test` suite passed (9 tests).
  - Follow-up Actions: Start Scenario B retention policy design and data model extension.

- 2026-08-05:
  - Task ID: SCENARIO-B-RETENTION-SLICE-1
  - Prompt Intent: Implement retention policy behavior with archive handling that does not break verification.
  - Constraints Provided: Preserve append-only semantics and keep chain verification intact.
  - AI Output Summary: Added retention run endpoint, archival update query, includeArchived query option, and integration test coverage for retention + verify behavior.
  - Disposition: ACCEPTED
  - Human Rationale: Delivers the first concrete Scenario B capability with test-backed behavior.
  - Validation Performed: Full `mvn test` suite passed (10 tests).
  - Follow-up Actions: Implement structured redaction and bulk export slices.

- 2026-08-05:
  - Task ID: SCENARIO-B-REDACTION-SLICE-1
  - Prompt Intent: Implement structured redaction with approval metadata while preserving tamper-evidence verification.
  - Constraints Provided: Keep chain verification deterministic and avoid mutating hash-source payload values.
  - AI Output Summary: Added redaction endpoint, redaction audit persistence, proof-artifact generation, and query-time payload masking for configured fields.
  - Disposition: ACCEPTED
  - Human Rationale: Satisfies BL-204/BL-205 with behavior verified through integration tests.
  - Validation Performed: Full `mvn test` suite passed (11 tests).
  - Follow-up Actions: Implement bulk export endpoint and offline bundle verifier.

- 2026-08-05:
  - Task ID: SCENARIO-B-EXPORT-SLICE-1
  - Prompt Intent: Implement bulk export bundles with independent verification support.
  - Constraints Provided: Maintain deterministic metadata/checksum behavior and keep redacted values masked in exports.
  - AI Output Summary: Added export bundle endpoint, checksum computation, offline bundle verification utility endpoint, and integration tests for valid and tampered bundles.
  - Disposition: ACCEPTED
  - Human Rationale: Completes BL-206/BL-207 and closes Scenario B integration scope in BL-208.
  - Validation Performed: Full `mvn test` suite passed (13 tests).
  - Follow-up Actions: Move to Scenario C ambiguity clarification and scoped compliance reporting.
