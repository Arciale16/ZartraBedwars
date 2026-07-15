# M06 compatibility and world API reference

## Compatibility and versioning

M06 is additive over the M02-M05 public API. Neutral contracts compile to Java
8 bytecode and are recorded in `build/api-signature-baseline-m06.txt`. Modern
implementation-facing public types compile to Java 21 and are recorded
separately in `build/api-signature-baseline-m06-modern.txt`. Removal or an
incompatible descriptor change requires the established major-version and
deprecation process. Public methods accept no `null` unless explicitly stated;
expected runtime failure is a typed outcome, not a generic exception.

## Public packages

| Package | Main public contracts | Threading |
|---|---|---|
| `io.zartra.bedwars.compat.api` | `SemanticKey`, `CompatibilityMapping`, `CompatibilityOutcome`, `CompatibilityValidation`, `CompatibilityAdapter`, `SemanticMappingRegistry` | Immutable values and registry snapshots are thread-safe; adapter lifecycle remains asynchronous |
| `io.zartra.bedwars.compat.modern` | `PrimarySemanticMappings`, `Paper121CompatibilityAdapter` | Mapping resolution is non-blocking; no world mutation |
| `io.zartra.bedwars.world.api` | `WorldKey`, `WorldOperation`, `WorldOperationResult`, `WorldProvider` and its bounded `Plan`, `Step`, `StepResult`, `ResourceSnapshot` | `WORKER` steps may do bounded I/O; `OWNER` steps alone may touch world/entity state |
| `io.zartra.bedwars.world.orchestration` | `WorldOrchestrator`, `OperationHandle`, immutable accounting `Snapshot` | Submission is non-blocking; completion is asynchronous; drain is a bounded control operation and must not run on a Minecraft owner thread |
| `io.zartra.bedwars.paper.bootstrap` | `PaperFoundationSettings`, `PaperFoundationRuntime`, `PaperOwnerThreadDispatcher`, `ZartraBedWarsPlugin` | Bootstrap start is owner-thread; runtime stop schedules blocking drain off-owner |
| `io.zartra.bedwars.paper.world` | `PaperNativeWorldProvider` | Filesystem steps reject owner-thread execution; Paper mutations reject worker execution |

## Outcome model

`CompatibilityOutcome` distinguishes native support, safe fallback, degraded
behavior and unsupported capability. Every result carries a semantic key and
stable reason; fallback/degraded values retain both preferred and selected
mapping information. `CompatibilityValidation` reports deterministic missing,
duplicate and malformed mapping errors. A failed update never replaces the
registry's last-known-good immutable snapshot.

`WorldOperationResult` always completes non-exceptionally for admitted runtime
work with `SUCCEEDED`, `FAILED`, `CANCELLED`, `TIMED_OUT` or `REJECTED`, a
stable reason, completed-step IDs, compensation completeness and final resource
snapshot. Programmer input errors are rejected synchronously by constructors.

## Extension rules

Later features consume semantic keys and `WorldProvider`; they may not import a
modern material, sound, particle, entity, packet, inventory or scheduler type.
Provider plans contain 1–16 immutable steps and must supply idempotent
compensation. A custom provider must pass the reusable contract suite before it
can be selected. M06 exposes no gameplay, arena, command, permission, GUI or
PlaceholderAPI surface.
