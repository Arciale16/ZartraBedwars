# M12 Phase 2 API

The Java 8 adapter entry points are in `io.zartra.bedwars.storage.sql`:

- `JdbcProgressionRepositories(int)` returns all eight neutral Phase 1 repository ports.
- `ProgressionSchemaMigrator(int)` exposes deterministic checksum and migration evidence.
- `JdbcProgressionTransactions(StorageEngine, int)` exposes XP projection, persistent-currency transaction, and purchase-settlement boundaries.

Repository calls are synchronous and potentially blocking. They run on bounded storage workers, never a Minecraft owner/tick thread. Every call requires an active JDBC-backed M04 `UnitOfWork`; callers control commit/rollback unless using the transaction coordinator. Failures use typed `Result` errors without vendor messages. History limits are 1–1000. Idempotency is enforced by database uniqueness plus transactional lookup. Optimistic conflicts fail closed. Reward registration persists intent only; delivery is not part of Phase 2.
