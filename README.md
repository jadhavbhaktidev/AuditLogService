# Audit Log Service (AI-Assisted Engineering Assignment)

This repository contains a production-style prototype of a tamper-evident audit log service.

## Quick Start

1. Install prerequisites listed in `docs/setup.md`.
2. Configure environment variables from `.env.example`.
3. Start the service.
4. Run tests.
5. Run the tamper-evidence demo flow.

## Repository Map

- `ATTESTATION.md`: Required candidate attestation.
- `docs/requirements-normalized.md`: Clarified and normalized requirements.
- `docs/architecture.md`: Data model, chain design, and trade-offs.
- `docs/scenarios/A-core.md`: Scenario A execution and validation.
- `docs/scenarios/B-extension.md`: Scenario B execution and validation.
- `docs/scenarios/C-ambiguous.md`: Scenario C clarification and scoped implementation.
- `docs/testing-strategy.md`: Unit/integration test coverage and gaps.
- `docs/ai-usage-log.md`: AI prompt traceability notes.
- `docs/final-engineering-summary.md`: Final rationale, risks, and limitations.
- `docs/backlog.md`: Issue-ready execution backlog.

## Required Evidence

- Commit history showing incremental engineering work.
- AI usage decisions: accepted/modified/rejected with rationale.
- Working APIs for write, query, verify.
- Tamper detection demonstration.
- Scenario B retention, redaction, and export evidence.
- Scenario C ambiguity handling evidence.

## Suggested Commit Rhythm

- Commit every 30 to 90 minutes.
- Keep each commit scoped to one task.
- Include test/docs updates with code changes when possible.
