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
