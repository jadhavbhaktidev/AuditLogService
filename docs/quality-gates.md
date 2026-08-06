# Quality Gates and CI

## Local Commands

1. Run full verification (tests + Checkstyle + SpotBugs):
   - `mvn verify`
   - Note: SpotBugs is skipped by default on local environments where newer JDK bytecode is incompatible.
   - To force SpotBugs locally on a compatible JDK: `mvn -Dspotbugs.skip=false verify`
2. Run dependency security scan:
   - `mvn -Psecurity-scan verify`

## CI Workflow

1. Workflow file:
   - `.github/workflows/ci.yml`
2. Triggers:
   - Push to `main`
   - Pull request targeting `main`
3. Jobs:
   - `build-and-test`: runs `mvn -Dspotbugs.skip=false verify` on JDK 21
   - `dependency-scan`: runs `mvn -Psecurity-scan verify`

## Implemented Static Analysis Rules

1. Checkstyle baseline checks:
   - Tabs not allowed
   - Unused imports
   - Star imports blocked
   - Braces required for control structures
2. SpotBugs configured with:
   - Effort `Max`
   - Threshold `Medium`
   - DTO package excluded via `config/spotbugs/exclude.xml`

## Security Scan Baseline

1. OWASP Dependency Check plugin configured under Maven profile `security-scan`.
2. Build fails for vulnerabilities with CVSS score >= 7.
