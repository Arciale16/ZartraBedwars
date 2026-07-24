# M15 Phase 2 — Statistics Runtime and Persistence

Implements the M15 projection engine for `ZBW-STATS-001`, `ZBW-STATS-005` and the foundational persistence/recovery portions of `ZBW-STATS-002`, `ZBW-STATS-004`, `ZBW-STATS-006..008`.

`StatisticsProjectionEngine` consumes only the normalized M08/M11/M12 event boundary, claims an idempotency key in the caller-owned M04 transaction, applies the configured SUM/MAXIMUM/LATEST aggregate rule and saves through optimistic revision control. `JdbcStatisticsStore` uses bounded prepared statements and `StatisticsSchemaMigrator` owns checksum-locked schema version 15.

No PlaceholderAPI, Paper/UI/command surface, Replay, web endpoint, Redis/distributed rank, hologram or external provider is included.
