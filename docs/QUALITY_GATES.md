# Quality Gates

**Status:** Accepted
**Decision:** RC-062
**Requirement:** `ZBW-READY-017`

| Gate | Mandatory threshold |
|---|---|
| Domain/application coverage | ≥90% line and ≥85% branch per module |
| Adapter/UI/integration coverage | ≥80% line and ≥70% branch per module; generated DTO/config code may be separately reported, never silently excluded |
| Critical invariants | 100% line/branch for authorization, reward/currency/stat idempotency, match transitions, legal hold/deletion, network authentication and script capability checks |
| Mutation testing | ≥80% mutation score on critical domain/security/transaction policies; no surviving mutation that removes an authorization or idempotency check |
| Regression | Zero failed deterministic tests, flaky-test quarantine or ignored test on a release branch |
| Style/static | Zero Checkstyle errors; zero new high-confidence SpotBugs defects; warnings have owner, rationale and expiry |
| Dependencies | Zero dynamic/range/SNAPSHOT release coordinates; locked SBOM; zero critical/high known vulnerability without owner-approved exception |
| Vulnerability exception | Written exploitability analysis, compensating controls, owner/security approval, ≤30-day expiry for critical and ≤90-day expiry for high; release notes disclose operational mitigation |
| API/architecture | No forbidden dependency, no platform import in core, no undocumented public binary break, one-major-version deprecation window |
| Documentation/traceability | 100% semantic and atomic coverage; no orphan ID/surface/test/doc; every validator passes |
| Production completeness | Zero TODO/FIXME/stub/fake provider/mock-only production path; no swallowed exception or unbounded resource |

The build publishes machine-readable and human-readable reports. Exclusions are explicit files/lines with stable issue, owner and expiry. Generated code is verified by schema/golden tests. A failing gate blocks milestone exit; only a time-bounded owner-approved security exception is allowed, never a coverage or scope exception that hides missing behavior.
