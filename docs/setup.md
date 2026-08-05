# Setup Instructions

## Prerequisites

- Git
- Java 21 (JDK)
- Maven 3.9+
- PostgreSQL 16+
- Docker Desktop (recommended for local PostgreSQL and test dependencies)

## Local Run Steps

1. Clone the private repository.
2. Create database and user in PostgreSQL:
	- Database: `audit_log_service`
	- User: `audit_user`
3. Configure application settings in `src/main/resources/application-local.yml` (or environment variables).
4. Run schema migrations via Flyway (auto-run on app start, if enabled).
5. Start the service:
	- `mvn spring-boot:run -Dspring-boot.run.profiles=local`
6. Run tests:
	- `mvn test`
7. Run integration tests (if split profile is used):
	- `mvn verify`
8. Execute the tamper-evidence demo sequence.

## Demo Sequence

1. Create events through write API.
2. Query with multiple filter combinations.
3. Verify chain: expect `intact=true`.
4. Modify one stored record directly.
5. Verify chain again: expect first inconsistency reported.

## Notes

- Keep this file aligned with actual commands you implement.
- Add OS-specific instructions if required.
- Assistant workflow rule: no commit or push is executed without explicit user approval in chat.
