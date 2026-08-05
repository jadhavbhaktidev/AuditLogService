# Testing Strategy, Limitations, and Trade-Offs

## Test Layers

1. Unit tests
   - Canonical serializer
   - Hash computation
   - Verification engine edge cases
2. Integration tests
   - API write/query/verify flows
   - Tamper detection after direct data mutation
   - Retention/archive verification behavior
   - Redaction behavior and invariants
   - Export bundle offline verification
3. Contract tests
   - Request validation and error schema
   - Pagination contract

## Minimum Critical Cases

1. First-record genesis handling.
2. Sequence gap and reorder detection.
3. Prev-hash mismatch detection.
4. Record-hash mismatch detection.
5. Mixed filter queries with stable pagination.

## Quality Gates

1. Lint and static checks must pass.
2. Unit and integration tests must pass in CI.
3. Security dependency scan reports reviewed.

## Known Prototype Limitations (Template)

1. Full-chain verification cost at very large volumes.
2. Single-region assumptions.
3. Limited operational hardening.
