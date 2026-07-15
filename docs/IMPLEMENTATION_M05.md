# Milestone 5 implementation evidence

## Scope

M05 implements only `ZBW-ARC-005`, `ZBW-ARC-006`, `ZBW-OPS-005`,
`ZBW-OPS-006` and the scheduler/recovery-marker foundation of `ZBW-GAME-010`.
It materializes `zbw-observability` and extends the Java-8 public API and platform-neutral
application module. No Bukkit/Paper/NMS, proxy, Redis, JDBC, filesystem, world, arena, gameplay or
M06 compatibility adapter is present.

## Delivered behavior

- Typed task identity, descriptors, cancellation, structured outcomes, scheduler accounting,
  owner-dispatch contracts and fail-fast thread guards.
- Fixed workers, bounded queues, explicit saturation rejection, cooperative deadlines,
  cancellation, failure isolation, graceful drain and force-stop accounting.
- Terminal outcomes are published only after accepted/completed/failed/rejected/cancelled
  accounting and structured failure delivery are coherent; the Linux CI race has regression
  coverage in `M05ApplicationTest`.
- Ordered startup/rollback/drain/shutdown lifecycle and bounded retry/circuit-breaker policies.
- Compare-and-set recovery markers, ordered recovery steps, duplicate-completion fencing,
  MANUAL_REQUIRED degradation and a recovery event contract.
- Bounded health and metric registries, secret-safe failure metadata, allowlist-only diagnostic
  export and zeroizable seeded redaction.
- Bounded extension-safe Plugin Doctor execution with failure isolation and sanitized evidence.
- Eleven M05 performance configuration keys, strict JavaDoc, additive M05 binary baseline,
  deterministic architecture validator and five-JDK CI matrix.

## Verification evidence

The twelve-project reactor executes 101 deterministic tests with zero failures, errors or skips.
M05 application tests cover owner violations, saturation, queued/running cancellation, pre/post
deadline expiry, exception and sink isolation, graceful/forced shutdown, retry/circuit bounds,
startup order, rollback, invalid state, deadline force, marker conflicts and recovery degradation.
Observability tests cover health-source failure, duplicate/capacity bounds, metric cardinality,
allowlist/classification filtering, seeded-secret redaction, unsafe sanitizer rejection, Doctor
check failure and evidence bounds.

| Module | Line coverage | Branch coverage | Required | Checkstyle | SpotBugs |
|---|---:|---:|---|---|---|
| `zbw-application` | 96.69% | 90.40% | 90% / 85% | 0 violations | 0 findings |
| `zbw-observability` | 98.31% | 85.58% | 80% / 70% | 0 violations | 0 findings |

The clean checksum-locked build and quality profile pass. The API baseline contains 270 public JVM
classes at class-major 52 and preserves every M02/M03/M04 signature. Dependency/licence/SBOM and
notice validation add no production dependency: observability uses only `zbw-api`,
`zbw-application` and test-scoped approved JUnit.

| Compile JDK | Exact Temurin release | Reactor | Tests | Skipped |
|---:|---|---|---:|---:|
| 8 | 8u442-b06 | PASS | 101 | 0 |
| 11 | 11.0.26+4 | PASS | 101 | 0 |
| 16 | 16.0.2+7 | PASS | 101 | 0 |
| 17 | 17.0.14+7 | PASS | 101 | 0 |
| 21 | 21.0.6+7 | PASS | 101 | 0 |

Generated evidence comprises twelve thin Maven artifacts, JaCoCo HTML/XML reports, Surefire XML,
`target/zartrabedwars-m05-javadoc.zip`, the M05 API baseline and governance output. The exact
Temurin 8/11/16/17/21 matrix and GitHub Actions evidence are recorded by the M05 workflow.

## Boundary

M06 has not started. Owner-thread dispatch remains an adapter contract until M06; gameplay recovery
steps remain M08; provider dashboards and operational command/GUI integration remain M24.
