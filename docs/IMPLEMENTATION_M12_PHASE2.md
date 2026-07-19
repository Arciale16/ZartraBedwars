# M12 Phase 2 — Storage and Persistence Foundation

Status: checkpoint implementation for `ZBW-PROG-001..010`, `ZBW-ECO-001..004`, and the M12 storage portions of `ZBW-STORE-001..012`.

`zbw-storage-sql` now depends on the neutral `zbw-progression` ports. `JdbcProgressionRepositories` supplies eight JDBC adapters, `ProgressionSchemaMigrator` installs checksum-locked schema version 12, and `JdbcProgressionTransactions` owns explicit atomic XP projection, persistent-currency, and M11 purchase-settlement boundaries. No SQL type crosses into `zbw-progression`; no Paper, command, GUI, statistics, PlaceholderAPI, proxy, provider, replay, Atlas, or compatibility implementation was added.

Operations use prepared statements, positive timeouts, caller-owned M04 `UnitOfWork` transactions, optimistic revisions, bounded history reads, immutable ledgers, and unique idempotency keys. XP projection claims the inbox event before aggregate, ledger, optional reward-intent, and outbox writes. Currency duplicates return prior ledger evidence. Purchase settlement references M11 purchases without recreating shop validation.

SQLite contracts cover migration replay/checksum stability, all repository ports, commit, rollback, optimistic conflict, duplicate suppression, restart recovery, deterministic histories, and entitlement/reward recovery. Existing M04 outbox/inbox, retry, retention, tombstone, backup, MySQL, and MariaDB infrastructure remains authoritative. External MySQL/MariaDB execution remains assigned to the container contract workflow; no local container engine was available.

Phase 3 reward delivery, levels/rewards policy, commands, GUIs, and Paper integration remain unstarted.
