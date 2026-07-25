# M15 Phase 2 — Statistics Runtime and Persistence

Implements the M15 projection engine for `ZBW-STATS-001`, `ZBW-STATS-005` and the foundational persistence/recovery portions of `ZBW-STATS-002`, `ZBW-STATS-004`, `ZBW-STATS-006..008`.

`StatisticsProjectionEngine` consumes only the normalized M08/M11/M12 event boundary, claims an idempotency key in the caller-owned M04 transaction, applies the configured SUM/MAXIMUM/LATEST aggregate rule and saves through optimistic revision control. `JdbcStatisticsStore` uses bounded prepared statements and `StatisticsSchemaMigrator` owns checksum-locked schema version 15.

The match-aggregate vertical slice extends that foundation with `MatchStatisticsProjection`
and `JdbcMatchStatisticsStore`. Match snapshots retain their optimistic
`RecordRevision` and audit metadata across restart-safe reads. The adapter performs
durable event claims, insert or revision-guarded update, and aggregate reads through
bounded prepared statements in the caller's `UnitOfWork`; it never changes
auto-commit state and never commits or rolls back the transaction. Schema version 15
owns `statistics_match_aggregates` and the primary-key indexes used for aggregate
lookup and durable claim detection.

`JdbcMatchStatisticsStoreTest` provides SQLite evidence for insert/load and update
round trips, process restart reads, duplicate claims, typed revision conflicts,
caller rollback, absent aggregates, checksum stability, malformed revisions and
deterministic reloads. Direct Java 8 compilation is also required for this slice.

No PlaceholderAPI, Paper/UI/command surface, Replay, web endpoint, Redis/distributed rank, hologram or external provider is included.
