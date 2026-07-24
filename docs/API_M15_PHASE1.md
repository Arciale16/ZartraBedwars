# M15 Phase 1 API

The `io.zartra.bedwars.statistics` Java-8-neutral API exposes only immutable values and ports.

- `model` contains typed definition IDs, category, audit metadata, player/match/team/season snapshots and scope identities.
- `integration.StatisticsEventAdapter` maps existing immutable M08 `MatchTransition`, M11 `PurchaseOutcome`, and M12 `ProjectionResult` contracts into M15 facts. It does not publish or mutate those source systems.
- `projection.StatisticProjection` requires a caller-owned M04 `UnitOfWork`, an idempotency key, typed result status, and explicitly bounded rebuild work.
- `repository.StatisticRepositories` contains JDBC-free ports only; implementations, migrations and query plans are deferred.
- `leaderboard.LeaderboardContracts` provides deterministic value ordering with canonical player-ID tie breaking and page sizes capped at 100.

Callers must invoke repository/projection operations off Minecraft owner threads. Public contracts use null rejection, typed `Result` failures where an operation can fail, and immutable values for thread-safe handoff. This phase is API additive and preserves the M11/M12 public contracts unchanged.
