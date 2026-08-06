# Scenario C - Ambiguous Compliance Reporting

## Initial Product Statement

Regulators need to be able to audit access to client account data.

## Ambiguities Identified

1. Which access actions count as auditable?
2. Required retention period and reporting granularity?
3. Required identities and role context?
4. Required evidence format for regulators?
5. Performance and response-time expectations?

## Clarified Requirement Statement (Working)

Provide a verifiable report of all accesses to client account data, including who accessed what, when, and why, with tamper-evident integrity validation and filterable retrieval by account, actor, and time range.

## Clarifications Finalized for Prototype

1. Access scope for this prototype is approximated by events with `resourceType = ACCOUNT`.
2. Report query requires explicit time window and bounded scope (`actorId` or `resourceId`).
3. Report output must include chain-integrity snapshot and per-record hash value.
4. Redaction policy from Scenario B applies to report payloads.
5. Output format is JSON API response for this iteration; CSV is deferred.

## Assumptions

1. Access events are already captured by Scenario A write path.
2. Report consumers require CSV or JSON export.
3. Role/context fields are present in payload schema.

## Scoped Implementation Plan

1. Add compliance-specific query endpoint or report mode.
2. Enforce required filters and date bounds.
3. Include integrity summary (record range + verification status).
4. Add tests for report completeness and filter accuracy.
5. Document deferred items and rationale.

## Implemented in This Slice

1. Added `GET /audit/compliance/report` endpoint.
2. Enforced required `from`/`to` bounds and scope filter rule.
3. Constrained report to `ACCOUNT` resource events.
4. Included integrity summary (`sourceChainIntact`, `sourceCheckedRecords`, sequence range).
5. Added integration tests for filter accuracy and invalid request combinations.

## Validation Evidence

1. Full `mvn test` suite passes after compliance report slice (15 tests).
2. Report excludes non-`ACCOUNT` resources.
3. Report reflects redacted payload masking where applicable.

## Out-of-Scope for Prototype

1. Regulatory workflow integration.
2. Jurisdiction-specific formatting packs.
3. Data lake ingestion pipelines.

## Deferred Scope Rationale

1. CSV output is deferred to keep this slice focused on contract clarity and integrity guarantees.
2. Jurisdiction templates are deferred because legal format requirements were not specified.
3. Real-time regulator push integrations are deferred due operational and security complexity.
