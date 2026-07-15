# M05 public API reference

## Compatibility policy

All M05 public types are platform-neutral Java 8 bytecode. The append-only binary baseline is
`build/api-signature-baseline-m05.txt`; M02, M03 and M04 signatures must remain present.
Expected operational failure is represented by immutable typed values, never generic public
exceptions. Public contracts accept no `null` unless explicitly documented. Deprecation requires
an additive compatibility window; removal requires a major API version and an accepted ADR.

## Packages

| Package | Public contract |
|---|---|
| `api.failure` | `FailureKind`, secret-safe `FailureReport`, non-blocking `FailureSink`. |
| `api.scheduler` | Task identity/descriptor/context, cooperative cancellation, scheduler admission/outcomes, accounting, owner dispatcher and fail-fast thread guard. |
| `api.lifecycle` | Idempotent component startup, drain, stop, force-stop and immutable reports. |
| `api.health` | Non-blocking health sources, status snapshots and bounded-label metrics. |
| `api.diagnostic` | Classified candidate fields, contributors, final-boundary sanitizer and deterministic export. |
| `api.doctor` | Extension-safe bounded Plugin Doctor checks and reports. |
| `api.recovery` | Compare-and-set recovery markers, marker store, ordered step, report and post-transition event. |
| `api.identity` | `TaskId` collision-resistant task identity. |

Scheduler operations execute on bounded workers. They must poll `CancellationToken` during long
work and may not mutate world, entity or inventory state directly. A Minecraft adapter supplied
by M06 or later implements `OwnerThreadDispatcher`; M05 does not claim a Paper implementation.
Lifecycle, recovery and Doctor methods run on bounded control workers and must honor their task
deadlines. Diagnostic contributors and health sources must be fast and non-blocking.

No API type imports Bukkit, Paper, NMS, proxy, Redis, JDBC, filesystem or runtime configuration.
No implementation class is exposed through the public contracts.
