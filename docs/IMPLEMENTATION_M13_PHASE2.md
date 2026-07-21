# M13 Phase 2 implementation

## Scope and requirements

Phase 2 implements the runtime and durable-state portions of `ZBW-PROG-009`,
`ZBW-PROG-010`, `ZBW-PROG-012`, `ZBW-PROG-013`, `ZBW-CONTENT-004..006` and
`ZBW-ADDON-081..091`. It extends the Phase 1 definitions without changing M08,
M11 or M12 behavior.

## Runtime

- `ObjectiveExecutionEngine` evaluates allow-listed event types and filters, performs bounded
  monotonic or repeatable accumulation, detects completion/expiration and rejects incompatible
  definition versions. `ObjectiveRuntimeState` carries optimistic revision, audit context and the
  last event key.
- `M13EventAdapter` maps configured M08 game, M11 settlement and M12 progression inputs into the
  shared objective fact model. It does not subscribe to or recreate those source pipelines.
- `M13ProjectionEngine` claims the source idempotency key and saves objective state inside the
  caller-owned M04 transaction. Duplicate replay returns durable duplicate evidence. Completion
  emits stable `M13RewardIntent` values for the existing M12 reward engine.
- `QuestRuntime`, `AchievementRuntime`, `ChallengeRuntime` and `BattlePassRuntime` implement
  assignment/activation, progress projection, completion, expiration/reset, chain advancement,
  hidden discovery, tier XP and free-track claim state. Payment, premium purchasing and cosmetics
  are deliberately absent.

## Persistence and recovery

`M13StateRepository` is a Java 8 neutral transaction-aware port. `JdbcM13StateRepository` is its
only JDBC adapter and stores objective, quest, achievement, challenge and season snapshots using
prepared statements and optimistic revisions. `M13SchemaMigrator` adds five state tables plus a
durable event-claim table as checksum-locked schema version 13, reusing M04/M12 transaction and
schema-history infrastructure. SQLite restart, rollback, duplicate claim and conflict tests provide
local recovery evidence. MySQL/MariaDB certification remains part of the M13 final external gate.

## Explicitly deferred

Commands, GUI, Paper projection, notifications, editors, starter content activation and complete
season rollover remain later M13 phases. Statistics (M15), PlaceholderAPI (M16), replay (M17),
Atlas (M18), distributed/proxy systems (M19/M20), external providers (M21) and compatibility (M22)
remain unchanged and unclaimed.
