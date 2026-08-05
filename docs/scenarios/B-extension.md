# Scenario B - Retention, Redaction, and Bulk Export

## Objective

Extend core system with retention/archive behavior, structured redaction, and verifiable export bundles.

## Task Breakdown

1. Add retention config and policy evaluator.
2. Implement archive marker/partition strategy.
3. Update verifier to handle legitimate archived records.
4. Design field-level redaction scheme preserving chain evidence.
5. Implement redaction workflow with approval metadata.
6. Add bulk export endpoint by actorId or resourceId.
7. Include chain metadata and signatures/checksums in bundle.
8. Implement offline bundle verification utility.
9. Add integration tests for retention boundaries.
10. Add integration tests for redaction behavior and verification.
11. Add integration tests for export and offline verify.

## Implementation Status (Current)

Implemented in this slice:

1. Retention run endpoint: `POST /audit/retention/run?days=`.
2. Service-level retention policy that archives records older than the configured/default window.
3. Query behavior update with `includeArchived` flag (default false).
4. Verify endpoint continues validating full chain including archived records.
5. API integration tests for retention archive behavior and verify integrity after archival.

## Validation Result (Current)

1. Full `mvn test` suite passes.
2. Archived records are excluded from default query results.
3. Including archived records via `includeArchived=true` returns full set.
4. Chain verification remains intact after legitimate archival.

## Redaction Design Notes (To Fill)

1. Sensitive fields list and policy source.
2. Replacement artifact format.
3. Hash-compatibility and proof strategy.
4. Operational approval and audit requirements.

## Done Criteria

1. Legitimate archive does not trigger false chain break.
2. Redacted payload values are not recoverable through API.
3. Tamper-evidence remains verifiable under chosen scheme.
4. Export bundles can be independently verified.
