# Sequential Commit Plan

This plan is prepared for later git commits and push, but no commit or push should be performed without explicit user approval.

## Commit 1 - bootstrap: Java/Spring/PostgreSQL scaffold

Includes:
- `pom.xml`
- `docker-compose.yml`
- `src/main/java/com/auditlogservice/AuditLogServiceApplication.java`
- `src/main/java/com/auditlogservice/web/HealthController.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/db/migration/V1__baseline.sql`
- `src/test/java/com/auditlogservice/AuditLogServiceApplicationTests.java`
- package placeholders under `src/main/java/com/auditlogservice/*`

Suggested message:
- `bootstrap: add spring boot postgres scaffold`

## Commit 2 - config: deterministic JSON and runtime wiring

Includes:
- `src/main/java/com/auditlogservice/config/JacksonConfig.java`
- any follow-up config changes needed for canonical serialization support

Suggested message:
- `config: add deterministic jackson bootstrap`

## Commit 3 - docs: traceability and setup alignment

Includes:
- `docs/ai-usage-log.md`
- any updates to `docs/setup.md`, `docs/architecture.md`, or `docs/requirements-normalized.md` needed to reflect the implemented scaffold

Suggested message:
- `docs: align setup and traceability with scaffold`

## Commit 4 - follow-on feature slice

Includes:
- Scenario A domain, repository, and API contracts when they are implemented

Suggested message:
- `feat: start scenario a audit record lifecycle`

## Approval Gate

Before creating any of the above commits or pushing to a remote, ask for explicit approval in chat.
