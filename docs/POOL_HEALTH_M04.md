# M04 pool and cache health

## Hikari topology

`SqlStorageConfiguration` permits 1..64 connections but deployment defaults remain those in `docs/OPERATIONAL_DEFAULTS.md`: shared SQL min 4/max 16 and scalable backend max 24. SQLite is forcibly exactly one connection and one JVM; scalable-proxy mode rejects it before a pool opens. Connection and validation timeouts are positive and bounded by configuration.

`JdbcStorageEngine.poolHealth()` exports only:

- active connections;
- idle connections;
- total connections;
- threads awaiting a connection;
- derived saturation (`waiting > 0` and no idle connection).

It never exports JDBC URLs, hosts, schemas, usernames, passwords, SQL/vendor messages, statements, player IDs or case IDs. M05 will bind these counters to the metrics/health runtime; M04 creates no polling thread.

## Alarms and response

- Nominal p99 pool wait is at most 10 ms.
- Warning at sustained 70% utilization, elevated at 80%, critical at 90% or any waiting thread with zero idle connections.
- Admission/backpressure belongs to M05. M04 fails within the configured connection timeout and classifies connectivity/serialization failures as retryable without an unbounded queue.
- On SQL authority loss, pause unsafe admission/purchase/claim/finalization paths as specified by network security; never treat the cache as durable authority.

## Cache health

`CaffeineVersionedCache` has an explicit 1..10,000,000 entry maximum, per-entry positive expiry, revision fencing and targeted/global invalidation. Estimated size is sanitized. Retention deletion/tombstone consumers must invalidate affected keys before acknowledging completion. Cache content is reconstructed from SQL after restart and has no RPO guarantee.

## Verification

Unit tests cover valid/impossible pool counter combinations, saturation, SQLite pool size, close behavior, cache bound, expiry, stale revision rejection and invalidation. Each RC-077 database job records HikariCP `active`, `idle`, `total`, `waiting` and `saturated` values after real repository activity and asserts a maximum of four connections. The certifier rejects missing/extra fields, impossible values, a total above four or any URL, host, schema, username, password, secret, token or JDBC text.

PR #5 run [29406777872](https://github.com/Arciale16/ZartraBedwars/actions/runs/29406777872) certified the same sanitized snapshot for both engines: active `0`, idle `2`, total `2`, waiting `0`, maximum `4`, saturated `false`. No credential or connection field appears in either uploaded artifact. Production thresholds remain assigned to M23/M24 load and operational drills.
