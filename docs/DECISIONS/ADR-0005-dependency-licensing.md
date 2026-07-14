# ADR-0005: Dependency Licensing and Redistribution

- **Status:** Accepted
- **Date:** 2026-07-14
- **Owner:** Project owner
- **Affected requirements:** `ZBW-LICENSE-001..007`, `ZBW-GOV-009`, `ZBW-OPS-008`
- **Resolves:** RC-076

## Context

The scope names many open-source and proprietary integrations, but exact dependency versions are not yet selected. Licence rights and obligations attach to exact artifacts/versions and packaging choices; project-level assumptions cannot safely authorize a release.

## Decision

Adopt the default-deny process in `docs/DEPENDENCY_LICENSE_AUDIT.md`. A dependency enters a build only with exact version/source/checksum, reviewed licence, redistribution/shading/modification/attribution/commercial-use decision and approved packaging scope.

Prefer official compile-only/provided APIs for optional plugins. Never commit or redistribute proprietary plugin binaries. Generate SBOM and `THIRD_PARTY_NOTICES.md` from approved rows; fail release on drift or missing obligations.

`UNSELECTED` is blocking, not an allowed version. RC-021/024/027 remain responsible for choosing exact versions/libraries before Java implementation; this is an explicit gate, not an unresolved licensing-policy decision.

## Alternatives considered

| Alternative | Benefit | Rejection reason |
|---|---|---|
| Approve by project name/latest | Low administration | Moving and legally unsafe |
| Bundle all APIs/plugins | Easy installation | Redistribution and proprietary-binary risk |
| Exact-artifact default-deny audit | Auditable and reproducible | Selected |

## Consequences

- Current selected dependency set is empty and no dependency is authorized.
- M01 must approve every exact build/test/runtime artifact before adding Java/build files.
- Version updates reopen only the affected audit rows.
- Release artifacts carry complete generated notices/SBOM.

## Acceptance evidence

Dependency lock/SBOM comparison, transitive licence scan, shaded-package scan, proprietary-binary denylist, reproducible notices and commercial-use review.

## Updated documents

PRD, architecture, dependency audit, third-party notices, milestones, traceability, risks and coverage report.
