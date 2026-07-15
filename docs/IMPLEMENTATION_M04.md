# Milestone 4 implementation evidence

## Scope

M04 implements the storage, migration, outbox/inbox and bounded-cache foundations allocated by `docs/MILESTONES.md`. Direct Requirement IDs are `ZBW-DEPLOY-007`, `ZBW-DEPLOY-008` and `ZBW-ARC-008`. It supplies only the durable-data foundations of `ZBW-PROG-001..014`, `ZBW-STATS-001..008`, `ZBW-REPLAY-001..010`, `ZBW-ATLAS-001..013`, `ZBW-READY-011`, `ZBW-READY-014` and `ZBW-READY-016`.

No scheduler, Minecraft adapter, proxy/Redis transport, feature repository, gameplay, progression behavior, statistics aggregation, replay recorder, Atlas workflow or M05 module is present. The generic storage foundation preserves those later owners.

## Modules and boundaries

- `zbw-storage-api` is Java-8 bytecode and depends only on `zbw-api` and `zbw-domain`. It contains no JDBC, Hikari, Flyway, cache implementation, filesystem, runtime configuration or platform dependency.
- `zbw-storage-sql` is Java-8 bytecode and is the only production module permitted to import JDBC or contain SQL. It provides Hikari-backed SQLite, MySQL and MariaDB adapters, the deterministic migration runner, a Java-8-linkable Flyway provider, Caffeine cache and recovery coordination.
- Both modules are synchronous/blocking by contract. Every call belongs on a bounded storage worker supplied by M05; owner/tick-thread use is forbidden. M04 does not create threads or executors.

The thin module JARs do not contain HikariCP, Caffeine, Flyway, database drivers or test libraries. The exact union of the JDK-specific Maven graphs contains 186 binary components and 591 JAR/POM files, each checksum/licence locked; product bundling is disabled for every row.

## Delivered behavior

- Immutable typed record keys, revisions, serialization payloads, transaction options, recovery objectives, migration plans and message envelopes.
- Thread-confined units of work with explicit commit, rollback and close-as-rollback semantics.
- Prepared-statement CRUD with optimistic concurrency and atomic business mutation/outbox or inbox operation in the caller's transaction.
- Uniqueness-based outbox/inbox idempotency, causal/event version metadata, deterministic ordering, bounded claim batches and expiring claims.
- SQLite single-connection serialized-writer enforcement and rejection of SQLite in scalable-proxy topology.
- MySQL/MariaDB bounded Hikari pools, read-only transaction hints, query timeouts and bounded deadlock retry classification.
- Versioned ordered schema history, SHA-256 checksum-drift rejection, restart-safe DDL and an actual Flyway classpath migration contract. External-engine unsafe DDL is marked backup-required.
- Bounded Caffeine cache with per-entry expiry, minimum-revision fencing, targeted/global invalidation and SQL authority.
- Retention classes, legal hold/release authorization records and duplicate-safe tombstones without evidence payload exposure.
- Backup/restore coordinator accepting only encrypted, independently validated provider evidence and enforcing zero committed-state RPO/15-minute SQL RTO declarations.
- Sanitized pool health counters; no URL, username, password, SQL error message, player ID or case ID appears in health output.

## Test and validation evidence

The M04 suite has 81 tests in the complete reactor: 79 execute locally with zero failures/errors and two digest-gated Testcontainers tests are skipped because this workstation has no Docker-compatible runtime or audited image environment variables. The executed SQLite tests cover migration/restart, crash rollback, CRUD, optimistic conflicts, transactions, outbox/inbox idempotency and ordering, retention/hold/tombstone semantics, cache expiry/versioning, recovery evidence, retry bounds and thread confinement. The MySQL and MariaDB contract suites are compiled and use only `@sha256` image references supplied through `ZBW_TEST_MYSQL_IMAGE` and `ZBW_TEST_MARIADB_IMAGE`.

Local quality evidence:

| Module | Line coverage | Branch coverage | Required | Checkstyle | SpotBugs |
|---|---:|---:|---|---|---|
| `zbw-storage-api` | 90.23% | 87.50% | 90% / 85% | 0 violations | 0 findings |
| `zbw-storage-sql` | 91.34% | 72.19% | 80% / 70% | 0 violations | 0 findings |

The clean offline reactor passed on every approved compile JDK. Each row compiled all production classes to Java 8 bytecode and discovered the same 81 tests:

| Compile JDK | Reactor | Tests | External skips |
|---:|---|---:|---:|
| 8 | PASS | 79 passed | 2 |
| 11 | PASS | 79 passed | 2 |
| 16 | PASS | 79 passed | 2 |
| 17 | PASS | 79 passed | 2 |
| 21 | PASS | 79 passed | 2 |

The first JDK 8 run exposed HikariCP's JDK-activated `slf4j-api:1.7.30` branch. The controlled acquisition process added its exact JAR, checksum and MIT licence evidence to the cross-JDK lock before the successful offline matrix was rerun.

The M04 deterministic entry point is `tools/validation/run_m04_validation.py`. It preserves M02/M03 binary signatures, verifies Java-8 class files, generates strict JavaDoc, confines SQL, validates exact dependencies/licences/SBOM, and runs every governance and 100%-coverage documentation gate.

## External exit gate

The SQLite and deterministic gates are verified locally. Full M04 exit remains externally pending until CI or an approved workstation runs both digest-pinned MySQL and MariaDB Testcontainers suites and records query plans under the certified server versions. This environmental gate does not authorize M05.
